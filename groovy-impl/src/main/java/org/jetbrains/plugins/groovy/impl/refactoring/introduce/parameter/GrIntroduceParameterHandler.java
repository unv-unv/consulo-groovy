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
package org.jetbrains.plugins.groovy.impl.refactoring.introduce.parameter;

import com.intellij.java.impl.ide.util.SuperMethodWarningUtil;
import com.intellij.java.language.psi.PsiMethod;
import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.codeEditor.SelectionModel;
import consulo.dataContext.DataContext;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.introduce.IntroduceTargetChooser;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.project.Project;
import consulo.ui.ex.popup.JBPopup;
import consulo.util.lang.function.PairFunction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.plugins.groovy.impl.refactoring.GrRefactoringError;
import org.jetbrains.plugins.groovy.impl.refactoring.GroovyRefactoringBundle;
import org.jetbrains.plugins.groovy.impl.refactoring.extract.GroovyExtractChooser;
import org.jetbrains.plugins.groovy.impl.refactoring.extract.InitialInfo;
import org.jetbrains.plugins.groovy.impl.refactoring.introduce.GrIntroduceHandlerBase;
import org.jetbrains.plugins.groovy.impl.refactoring.ui.MethodOrClosureScopeChooser;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrParameterListOwner;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.jetbrains.plugins.groovy.impl.refactoring.HelpID.GROOVY_INTRODUCE_PARAMETER;

/**
 * @author Maxim.Medvedev
 */
public class GrIntroduceParameterHandler implements RefactoringActionHandler, MethodOrClosureScopeChooser.JBPopupOwner {
  private JBPopup myEnclosingMethodsPopup;

  public void invoke(final @Nonnull Project project, final Editor editor, final PsiFile file, final @Nullable DataContext dataContext) {
    final SelectionModel selectionModel = editor.getSelectionModel();
    if (!selectionModel.hasSelection()) {
      final int offset = editor.getCaretModel().getOffset();

      final List<GrExpression> expressions = GrIntroduceHandlerBase.collectExpressions(file, editor, offset, false);
      if (expressions.isEmpty()) {
        final GrVariable variable = GrIntroduceHandlerBase.findVariableAtCaret(file, editor, offset);
        if (variable == null || variable instanceof GrField || variable instanceof GrParameter) {
          selectionModel.selectLineAtCaret();
        }
        else {
          final TextRange textRange = variable.getTextRange();
          selectionModel.setSelection(textRange.getStartOffset(), textRange.getEndOffset());
        }
      }
      else if (expressions.size() == 1) {
        final TextRange textRange = expressions.get(0).getTextRange();
        selectionModel.setSelection(textRange.getStartOffset(), textRange.getEndOffset());
      }
      else {
        IntroduceTargetChooser.showChooser(editor, expressions, new Consumer<GrExpression>() {
                                             public void accept(final GrExpression selectedValue) {
                                               invoke(project, editor, file, selectedValue.getTextRange().getStartOffset(), selectedValue.getTextRange().getEndOffset());
                                             }
                                           }, grExpression -> grExpression.getText()
        );
        return;
      }
    }
    invoke(project, editor, file, selectionModel.getSelectionStart(), selectionModel.getSelectionEnd());
  }

  private void invoke(final Project project, final Editor editor, PsiFile file, int startOffset, int endOffset) {
    try {
      final InitialInfo initialInfo = GroovyExtractChooser.invoke(project, editor, file, startOffset, endOffset, false);
      findScope(initialInfo, editor);
    }
    catch (GrRefactoringError e) {
      if (ApplicationManager.getApplication().isUnitTestMode()) throw e;
      CommonRefactoringUtil.showErrorHint(project,
                                          editor,
                                          e.getMessage(),
                                          RefactoringBundle.message("introduce.parameter.title"),
                                          GROOVY_INTRODUCE_PARAMETER);
    }
  }

  private void findScope(@Nonnull final InitialInfo initialInfo, @Nonnull final Editor editor) {
    PsiElement place = initialInfo.getContext();
    final List<GrParameterListOwner> scopes = new ArrayList<GrParameterListOwner>();
    while (true) {
      final GrParameterListOwner parent = PsiTreeUtil.getParentOfType(place, GrMethod.class, GrClosableBlock.class);
      if (parent == null) break;
      scopes.add(parent);
      place = parent;
    }

    if (scopes.size() == 0) {
      throw new GrRefactoringError(GroovyRefactoringBundle.message("there.is.no.method.or.closure"));
    }
    else if (scopes.size() == 1 || ApplicationManager.getApplication().isUnitTestMode()) {
      final GrParameterListOwner owner = scopes.get(0);
      final PsiElement toSearchFor;
      if (owner instanceof GrMethod) {
        toSearchFor = SuperMethodWarningUtil.checkSuperMethod((PsiMethod)owner, RefactoringBundle.message("to.refactor"));
        if (toSearchFor == null) return; //if it is null, refactoring was canceled
      }
      else {
        toSearchFor = MethodOrClosureScopeChooser.findVariableToUse(owner);
      }
      showDialog(new IntroduceParameterInfoImpl(initialInfo, owner, toSearchFor));
    }
    else {
      myEnclosingMethodsPopup =
        MethodOrClosureScopeChooser.create(scopes, editor, this, new PairFunction<GrParameterListOwner, PsiElement, Object>() {
          @Override
          public Object fun(GrParameterListOwner owner, PsiElement element) {
            showDialog(new IntroduceParameterInfoImpl(initialInfo, owner, element));
            return null;
          }
        });
      EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, myEnclosingMethodsPopup);
    }
  }

  @Override
  public JBPopup get() {
    return myEnclosingMethodsPopup;
  }


  //method to hack in tests
  protected void showDialog(IntroduceParameterInfo info) {
    new GrIntroduceParameterDialog(info).show();
  }


  @Override
  public void invoke(@Nonnull Project project, @Nonnull PsiElement[] elements, DataContext dataContext) {
    // Does nothing
  }
}
