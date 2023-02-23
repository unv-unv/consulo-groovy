/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.unwrap;

import com.intellij.java.language.psi.*;
import consulo.language.editor.CodeInsightBundle;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrAnonymousClassDefinition;

import java.util.List;

public class GroovyAnonymousUnwrapper extends GroovyUnwrapper {
  public GroovyAnonymousUnwrapper() {
    super(CodeInsightBundle.message("unwrap.anonymous"));
  }

  public boolean isApplicableTo(PsiElement e) {
    return e instanceof GrAnonymousClassDefinition
           && ((GrAnonymousClassDefinition)e).getMethods().length <= 1;
  }

  @Override
  public PsiElement collectAffectedElements(PsiElement e, List<PsiElement> toExtract) {
    super.collectAffectedElements(e, toExtract);
    return findElementToExtractFrom(e);
  }

  @Override
  protected void doUnwrap(PsiElement element, Context context) throws IncorrectOperationException
  {
    PsiElement from = findElementToExtractFrom(element);

    for (PsiMethod m : ((PsiAnonymousClass)element).getMethods()) {
      //context.extractFromCodeBlock(m.getBody(), from);
    }

    PsiElement next = from.getNextSibling();
    if (next instanceof PsiJavaToken && ((PsiJavaToken)next).getTokenType() == JavaTokenType.SEMICOLON) {
      context.deleteExactly(from.getNextSibling());
    }
    context.deleteExactly(from);
  }

  private static PsiElement findElementToExtractFrom(PsiElement el) {
    if (el.getParent() instanceof PsiNewExpression) el = el.getParent();
    el = findTopmostParentOfType(el, PsiMethodCallExpression.class);
    el = findTopmostParentOfType(el, PsiAssignmentExpression.class);
    el = findTopmostParentOfType(el, PsiDeclarationStatement.class);

    while (el.getParent() instanceof PsiExpressionStatement) {
      el = el.getParent();
    }

    return el;
  }

  private static PsiElement findTopmostParentOfType(PsiElement el, Class<? extends PsiElement> clazz) {
    while (true) {
      @SuppressWarnings({"unchecked"})
      PsiElement temp = PsiTreeUtil.getParentOfType(el, clazz, true, PsiAnonymousClass.class);
      if (temp == null || temp instanceof PsiFile) return el;
      el = temp;
    }
  }
}