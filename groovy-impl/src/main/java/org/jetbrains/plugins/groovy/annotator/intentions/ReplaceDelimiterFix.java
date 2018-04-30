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
package org.jetbrains.plugins.groovy.annotator.intentions;

import javax.annotation.Nonnull;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrForStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.clauses.GrForClause;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.clauses.GrForInClause;

/**
 * @author Max Medvedev
 */
public class ReplaceDelimiterFix implements IntentionAction {
  @Nonnull
  @Override
  public String getText() {
    return "Replace ':' with 'in'";
  }

  @Nonnull
  @Override
  public String getFamilyName() {
    return "Replace for-each operator";
  }

  @Override
  public boolean isAvailable(@Nonnull Project project, Editor editor, PsiFile file) {
    return true;
  }

  @Override
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
    PsiElement at = file.findElementAt(editor.getCaretModel().getOffset());
    GrForStatement forStatement = PsiTreeUtil.getParentOfType(at, GrForStatement.class);
    if (forStatement == null) return;
    GrForClause clause = forStatement.getClause();
    if (clause instanceof GrForInClause) {
      GroovyFile f = GroovyPsiElementFactory.getInstance(project).createGroovyFile("in", false, null);
      PsiElement child = f.getFirstChild().getFirstChild();
      PsiElement delimiter = ((GrForInClause)clause).getDelimiter();
      delimiter.replace(child);
    }
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}
