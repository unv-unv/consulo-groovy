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

package org.jetbrains.plugins.groovy.lang.psi.api.statements;

import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.plugins.groovy.lang.psi.api.formatter.GrControlStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

/**
 * @autor: ilyas
 */
public interface GrIfStatement extends GrStatement, GrControlStatement {

    @javax.annotation.Nullable
    GrExpression getCondition();

    @javax.annotation.Nullable
    GrStatement getThenBranch();

    @javax.annotation.Nullable
    GrStatement getElseBranch();

    <T extends GrStatement> T replaceThenBranch(T newBranch) throws IncorrectOperationException;

    <T extends GrStatement> T replaceElseBranch(T newBranch) throws IncorrectOperationException;

    @javax.annotation.Nullable
    PsiElement getElseKeyword();

    @javax.annotation.Nullable
    PsiElement getRParenth();

    @javax.annotation.Nullable
    PsiElement getLParenth();
}
