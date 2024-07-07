/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.impl.annotator.intentions;

import consulo.codeEditor.Editor;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.jetbrains.plugins.groovy.GroovyBundle;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrApplicationStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCommandArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

/**
 * @author Max Medvedev
 */
public class AddParenthesesFix implements IntentionAction {
  private static final Logger LOG = Logger.getInstance(AddParenthesesFix.class);

  @Nonnull
  @Override
  public String getText() {
    return GroovyBundle.message("add.parentheses");
  }

  @Nonnull
  //@Override
  public String getFamilyName() {
    return GroovyBundle.message("add.parentheses.to.command.method.call");
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    final int offset = editor.getCaretModel().getOffset();
    final PsiElement at = file.findElementAt(offset);
    final GrCommandArgumentList argList = PsiTreeUtil.getParentOfType(at, GrCommandArgumentList.class);
    return argList != null;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    final int offset = editor.getCaretModel().getOffset();
    final PsiElement at = file.findElementAt(offset);
    final GrCommandArgumentList argList = PsiTreeUtil.getParentOfType(at, GrCommandArgumentList.class);
    if (argList == null) return;

    final PsiElement parent = argList.getParent();
    LOG.assertTrue(parent instanceof GrApplicationStatement);

    final GrExpression newExpr;
    try {
      newExpr = GroovyPsiElementFactory.getInstance(project)
                                       .createExpressionFromText(((GrApplicationStatement)parent).getInvokedExpression()
                                                                                                 .getText() + '(' + argList.getText() + ')');
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
      return;
    }

    parent.replace(newExpr);
    editor.getCaretModel().moveToOffset(offset + 1);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
