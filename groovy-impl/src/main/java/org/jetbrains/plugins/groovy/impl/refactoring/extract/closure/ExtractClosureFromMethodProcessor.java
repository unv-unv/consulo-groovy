/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.plugins.groovy.impl.refactoring.extract.closure;

import com.intellij.java.impl.refactoring.IntroduceParameterRefactoring;
import com.intellij.java.impl.refactoring.introduceParameter.ChangedMethodCallInfo;
import com.intellij.java.impl.refactoring.introduceParameter.ExternalUsageInfo;
import com.intellij.java.impl.refactoring.introduceParameter.InternalUsageInfo;
import com.intellij.java.impl.refactoring.introduceParameter.IntroduceParameterData;
import com.intellij.java.impl.refactoring.util.usageInfo.DefaultConstructorImplicitUsageInfo;
import com.intellij.java.impl.refactoring.util.usageInfo.NoConstructorClassUsageInfo;
import com.intellij.java.indexing.search.searches.MethodReferencesSearch;
import com.intellij.java.indexing.search.searches.OverridingMethodsSearch;
import com.intellij.java.language.psi.*;
import consulo.application.ApplicationManager;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.ui.ConflictsDialog;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.usage.UsageInfo;
import consulo.usage.UsageViewUtil;
import consulo.util.collection.MultiMap;
import consulo.util.collection.primitive.ints.IntList;
import consulo.util.lang.ref.Ref;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrOpenBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrStatementOwner;
import org.jetbrains.plugins.groovy.impl.refactoring.GroovyRefactoringBundle;
import org.jetbrains.plugins.groovy.impl.refactoring.GroovyRefactoringUtil;
import org.jetbrains.plugins.groovy.impl.refactoring.extract.ExtractUtil;
import org.jetbrains.plugins.groovy.impl.refactoring.introduce.parameter.FieldConflictsResolver;
import org.jetbrains.plugins.groovy.impl.refactoring.introduce.parameter.GrExpressionWrapper;
import org.jetbrains.plugins.groovy.impl.refactoring.introduce.parameter.GrIntroduceParameterSettings;
import org.jetbrains.plugins.groovy.impl.refactoring.util.AnySupers;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.intellij.java.impl.refactoring.introduceParameter.IntroduceParameterUtil.changeMethodSignatureAndResolveFieldConflicts;
import static com.intellij.java.impl.refactoring.introduceParameter.IntroduceParameterUtil.processUsages;
import static org.jetbrains.plugins.groovy.impl.refactoring.introduce.parameter.GroovyIntroduceParameterUtil.*;

/**
 * @author Max Medvedev
 */
public class ExtractClosureFromMethodProcessor extends ExtractClosureProcessorBase {

  private final GrMethod myMethod;
  private final GrStatementOwner myDeclarationOwner;

  public ExtractClosureFromMethodProcessor(@Nonnull GrIntroduceParameterSettings helper) {
    super(helper);
    myDeclarationOwner = GroovyRefactoringUtil.getDeclarationOwner(helper.getStatements()[0]);
    myMethod = (GrMethod)myHelper.getToReplaceIn();
  }

  @Override
  protected boolean preprocessUsages(Ref<UsageInfo[]> refUsages) {
    UsageInfo[] usagesIn = refUsages.get();
    MultiMap<PsiElement, String> conflicts = new MultiMap<PsiElement, String>();
    final GrStatement[] statements = myHelper.getStatements();

    for (GrStatement statement : statements) {
      detectAccessibilityConflicts(statement, usagesIn, conflicts, false, myProject);
    }

    for (UsageInfo info : usagesIn) {
      if (info instanceof OtherLanguageUsageInfo) {
        final String lang = CommonRefactoringUtil.htmlEmphasize(info.getElement().getLanguage().getDisplayName());
        conflicts.putValue(info.getElement(), GroovyRefactoringBundle.message("cannot.process.usage.in.language.{0}", lang));
      }
    }

    if (!myMethod.hasModifierProperty(PsiModifier.PRIVATE)) {
      final AnySupers anySupers = new AnySupers();
      for (GrStatement statement : statements) {
        statement.accept(anySupers);
      }
      if (anySupers.containsSupers()) {
        for (UsageInfo usageInfo : usagesIn) {
          if (!(usageInfo.getElement() instanceof PsiMethod) && !(usageInfo instanceof InternalUsageInfo)) {
            if (!PsiTreeUtil.isAncestor(myMethod.getContainingClass(), usageInfo.getElement(), false)) {
              conflicts.putValue(statements[0], RefactoringBundle
                .message("parameter.initializer.contains.0.but.not.all.calls.to.method.are.in.its.class",
                         CommonRefactoringUtil.htmlEmphasize(PsiKeyword.SUPER)));
              break;
            }
          }
        }
      }
    }

    if (!conflicts.isEmpty() && ApplicationManager.getApplication().isUnitTestMode()) {
      throw new ConflictsInTestsException(conflicts.values());
    }

    if (!conflicts.isEmpty()) {
      final ConflictsDialog conflictsDialog = prepareConflictsDialog(conflicts, usagesIn);
      conflictsDialog.show();
      if (!conflictsDialog.isOK()) {
        if (conflictsDialog.isShowConflicts()) prepareSuccessful();
        return false;
      }
    }

    prepareSuccessful();
    return true;
  }

  @Nonnull
  @Override
  protected UsageInfo[] findUsages() {
    List<UsageInfo> result = new ArrayList<UsageInfo>();

    final PsiMethod toSearchFor = (PsiMethod)myHelper.getToSearchFor();

    for (PsiReference ref1 : MethodReferencesSearch.search(toSearchFor, GlobalSearchScope.projectScope(myProject), true)) {
      PsiElement ref = ref1.getElement();
      if (ref.getLanguage() != GroovyFileType.GROOVY_LANGUAGE) {
        result.add(new OtherLanguageUsageInfo(ref1));
        continue;
      }

      if (ref instanceof PsiMethod && ((PsiMethod)ref).isConstructor()) {
        DefaultConstructorImplicitUsageInfo implicitUsageInfo =
          new DefaultConstructorImplicitUsageInfo((PsiMethod)ref, ((PsiMethod)ref).getContainingClass(), toSearchFor);
        result.add(implicitUsageInfo);
      }
      else if (ref instanceof PsiClass) {
        result.add(new NoConstructorClassUsageInfo((PsiClass)ref));
      }
      else if (!PsiTreeUtil.isAncestor(myMethod, ref, false)) {
        result.add(new ExternalUsageInfo(ref));
      }
      else {
        result.add(new ChangedMethodCallInfo(ref));
      }
    }

    Collection<PsiMethod> overridingMethods = OverridingMethodsSearch.search(toSearchFor, true).findAll();

    for (PsiMethod overridingMethod : overridingMethods) {
      result.add(new UsageInfo(overridingMethod));
    }

    final UsageInfo[] usageInfos = result.toArray(new UsageInfo[result.size()]);
    return UsageViewUtil.removeDuplicatedUsages(usageInfos);
  }


  @Override
  protected void performRefactoring(UsageInfo[] usages) {
    final IntroduceParameterData data = new IntroduceParameterDataAdapter();

    processUsages(usages, data);

    final PsiMethod toSearchFor = (PsiMethod)myHelper.getToSearchFor();

    final boolean methodsToProcessAreDifferent = myMethod != toSearchFor;
    if (myHelper.generateDelegate()) {
      generateDelegate(myMethod, data.getParameterInitializer(), myProject);
      if (methodsToProcessAreDifferent) {
        final GrMethod method = generateDelegate(toSearchFor, data.getParameterInitializer(), myProject);
        final PsiClass containingClass = method.getContainingClass();
        if (containingClass != null && containingClass.isInterface()) {
          final GrOpenBlock block = method.getBlock();
          if (block != null) {
            block.delete();
          }
        }
      }
    }

    // Changing signature of initial method
    // (signature of myMethodToReplaceIn will be either changed now or have already been changed)
    final FieldConflictsResolver fieldConflictsResolver = new FieldConflictsResolver(myHelper.getName(), myMethod.getBlock());
    changeMethodSignatureAndResolveFieldConflicts(new UsageInfo(myMethod), usages, data);
    if (methodsToProcessAreDifferent) {
      changeMethodSignatureAndResolveFieldConflicts(new UsageInfo(toSearchFor), usages, data);
    }

    // Replacing expression occurrences
    for (UsageInfo usage : usages) {
      if (usage instanceof ChangedMethodCallInfo) {
        PsiElement element = usage.getElement();

        processChangedMethodCall(element, myHelper, myProject);
      }
    }

    final GrStatement newStatement = ExtractUtil.replaceStatement(myDeclarationOwner, myHelper);
    /*
    if (myEditor != null) {
      PsiDocumentManager.getInstance(myProject).commitDocument(myEditor.getDocument());
      myEditor.getCaretModel().moveToOffset(ExtractUtil.getCaretOffset(newStatement));
    }*/

    fieldConflictsResolver.fix();
  }

  private class IntroduceParameterDataAdapter implements IntroduceParameterData {

    private final GrClosableBlock myClosure;
    private final GrExpressionWrapper myWrapper;

    private IntroduceParameterDataAdapter() {
      myClosure = generateClosure(ExtractClosureFromMethodProcessor.this.myHelper);
      myWrapper = new GrExpressionWrapper(myClosure);
    }

    @Nonnull
    @Override
    public Project getProject() {
      return myProject;
    }

    @Override
    public PsiMethod getMethodToReplaceIn() {
      return myMethod;
    }

    @Nonnull
    @Override
    public PsiMethod getMethodToSearchFor() {
      return (PsiMethod)myHelper.getToSearchFor();
    }

    @Override
    public ExpressionWrapper getParameterInitializer() {
      return myWrapper;
    }

    @Nonnull
    @Override
    public String getParameterName() {
      return myHelper.getName();
    }

    @Override
    public int getReplaceFieldsWithGetters() {
      return IntroduceParameterRefactoring.REPLACE_FIELDS_WITH_GETTERS_NONE; //todo add option to dialog
    }

    @Override
    public boolean isDeclareFinal() {
      return myHelper.declareFinal();
    }

    @Override
    public boolean isGenerateDelegate() {
      return false; //todo
    }

    @Nonnull
    @Override
    public PsiType getForcedType() {
      PsiType type = myHelper.getSelectedType();
      return type != null ? type : PsiType.getJavaLangObject(PsiManager.getInstance(myProject), GlobalSearchScope.allScope(myProject));
    }

    @Nonnull
    @Override
    public IntList getParametersToRemove() {
      return myHelper.parametersToRemove();
    }

  }
}
