/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

package org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.language.ast.IElementType;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiPolyVariantReference;

/**
 * @author ilyas
 */
public interface GrAssignmentExpression extends GrExpression, PsiPolyVariantReference {

  @Nonnull
  GrExpression getLValue();

  @Nullable
  GrExpression getRValue();

  IElementType getOperationTokenType();

  @Nonnull
  GroovyResolveResult[] multiResolve(boolean incompleteCode);

  PsiElement getOperationToken();
}
