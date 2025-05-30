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
package org.jetbrains.plugins.groovy.impl.formatter;

import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenType;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.codeStyle.PostFormatProcessorHelper;
import consulo.language.impl.psi.CodeEditUtil;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiManager;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.GroovyRecursiveElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.*;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrCodeBlock;

/**
 * @author Max Medvedev
 */
public class GroovyBraceEnforcer extends GroovyRecursiveElementVisitor {
  private static final Logger LOG = Logger.getInstance(GroovyBraceEnforcer.class);

  private PostFormatProcessorHelper myPostProcessor;

  public GroovyBraceEnforcer(CodeStyleSettings settings) {
    myPostProcessor = new PostFormatProcessorHelper(settings.getCommonSettings(GroovyFileType.GROOVY_LANGUAGE));
  }

  public TextRange processText(final GroovyFile source, final TextRange rangeToReformat) {
    myPostProcessor.setResultTextRange(rangeToReformat);
    source.accept(this);
    return myPostProcessor.getResultTextRange();
  }

  public PsiElement process(GroovyPsiElement formatted) {
    LOG.assertTrue(formatted.isValid());
    formatted.accept(this);
    return formatted;
  }

  private void replaceWithBlock(@Nonnull GrStatement statement, GrStatement blockCandidate) {
    if (!statement.isValid()) {
      LOG.assertTrue(false);
    }

    if (!checkRangeContainsElement(blockCandidate)) return;

    final PsiManager manager = statement.getManager();
    LOG.assertTrue(manager != null);
    GroovyPsiElementFactory factory = GroovyPsiElementFactory.getInstance(manager.getProject());

    String oldText = blockCandidate.getText();
    // There is a possible case that target block to wrap ends with single-line comment. Example:
    //     if (true) i = 1; // Cool assignment
    // We can't just surround target block of code with curly braces because the closing one will be treated as comment as well.
    // Hence, we perform a check if we have such situation at the moment and insert new line before the closing brace.
    StringBuilder buf = new StringBuilder(oldText.length() + 5);
    buf.append("{\n").append(oldText);
    buf.append("\n}");
    final int oldTextLength = statement.getTextLength();
    try {
      ASTNode newChild = SourceTreeToPsiMap.psiElementToTree(factory.createBlockStatementFromText(buf.toString(), null));
      ASTNode parent = SourceTreeToPsiMap.psiElementToTree(statement);
      ASTNode childToReplace = SourceTreeToPsiMap.psiElementToTree(blockCandidate);
      CodeEditUtil.replaceChild(parent, childToReplace, newChild);

      removeTailSemicolon(newChild, parent);
      CodeStyleManager.getInstance(statement.getProject()).reformat(statement, true);
    }
    catch (IncorrectOperationException e) {
      LOG.error(e);
    }
    finally {
      updateResultRange(oldTextLength, statement.getTextLength());
    }
  }

  private static void removeTailSemicolon(ASTNode newChild, ASTNode parent) {
    ASTNode semi = newChild.getTreeNext();
    while (semi != null && semi.getElementType() == TokenType.WHITE_SPACE && !semi.getText().contains("\n")) {
      semi = semi.getTreeNext();
    }

    if (semi != null && semi.getElementType() == GroovyTokenTypes.mSEMI) {
      parent.removeRange(newChild.getTreeNext(), semi.getTreeNext());
    }
  }


  protected void updateResultRange(final int oldTextLength, final int newTextLength) {
    myPostProcessor.updateResultRange(oldTextLength, newTextLength);
  }

  protected boolean checkElementContainsRange(final PsiElement element) {
    return myPostProcessor.isElementPartlyInRange(element);
  }

  protected boolean checkRangeContainsElement(final PsiElement element) {
    return myPostProcessor.isElementFullyInRange(element);
  }

  private void processStatement(GrStatement statement, @Nullable GrStatement blockCandidate, int options) {
    if (blockCandidate instanceof GrCodeBlock || blockCandidate instanceof GrBlockStatement || blockCandidate == null) return;
    if (options == CommonCodeStyleSettings.FORCE_BRACES_ALWAYS ||
        options == CommonCodeStyleSettings.FORCE_BRACES_IF_MULTILINE && PostFormatProcessorHelper.isMultiline(statement)) {
      replaceWithBlock(statement, blockCandidate);
    }
  }

  @Override
  public void visitIfStatement(GrIfStatement statement) {
    if (checkElementContainsRange(statement)) {
      final SmartPsiElementPointer pointer =
        SmartPointerManager.getInstance(statement.getProject()).createSmartPsiElementPointer(statement);
      super.visitIfStatement(statement);
      statement = (GrIfStatement)pointer.getElement();
      if (statement == null) return;

      processStatement(statement, statement.getThenBranch(), myPostProcessor.getSettings().IF_BRACE_FORCE);
      final GrStatement elseBranch = statement.getElseBranch();
      if (!(elseBranch instanceof GrIfStatement) || !myPostProcessor.getSettings().SPECIAL_ELSE_IF_TREATMENT) {
        processStatement(statement, elseBranch, myPostProcessor.getSettings().IF_BRACE_FORCE);
      }
    }
  }

  @Override
  public void visitForStatement(GrForStatement statement) {
    if (checkElementContainsRange(statement)) {
      super.visitForStatement(statement);
      processStatement(statement, statement.getBody(), myPostProcessor.getSettings().FOR_BRACE_FORCE);
    }
  }

  @Override
  public void visitWhileStatement(GrWhileStatement statement) {
    if (checkElementContainsRange(statement)) {
      super.visitWhileStatement(statement);
      processStatement(statement, statement.getBody(), myPostProcessor.getSettings().WHILE_BRACE_FORCE);
    }
  }
}
