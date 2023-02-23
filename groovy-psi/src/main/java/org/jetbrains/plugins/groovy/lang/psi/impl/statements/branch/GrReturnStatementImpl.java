/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package org.jetbrains.plugins.groovy.lang.psi.impl.statements.branch;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.language.psi.PsiElement;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.branch.GrReturnStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;
import consulo.language.ast.ASTNode;

/**
 * @author ilyas
 */
public class GrReturnStatementImpl extends GroovyPsiElementImpl implements GrReturnStatement {
  public GrReturnStatementImpl(@Nonnull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(GroovyElementVisitor visitor) {
    visitor.visitReturnStatement(this);
  }

  public String toString() {
    return "RETURN statement";
  }

  @Override
  @Nullable
  public GrExpression getReturnValue() {
    return findExpressionChild(this);
  }

  @Nonnull
  @Override
  public PsiElement getReturnWord() {
    return findNotNullChildByType(GroovyTokenTypes.kRETURN);
  }
}
