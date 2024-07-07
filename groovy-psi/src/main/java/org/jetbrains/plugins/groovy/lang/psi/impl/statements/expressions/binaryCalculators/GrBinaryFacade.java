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
package org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.binaryCalculators;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import consulo.language.psi.PsiElement;
import consulo.language.ast.IElementType;

/**
 * Created by Max Medvedev on 12/20/13
 */
public interface GrBinaryFacade {

  @Nonnull
  GrExpression getLeftOperand();

  @Nullable
  GrExpression getRightOperand();

  @Nonnull
  IElementType getOperationTokenType();

  @Nonnull
  PsiElement getOperationToken();

  @Nonnull
  GroovyResolveResult[] multiResolve(final boolean incompleteCode);

  @Nonnull
  GrExpression getPsiElement();
}
