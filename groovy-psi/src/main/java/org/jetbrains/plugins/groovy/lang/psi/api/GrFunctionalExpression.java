// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.groovy.lang.psi.api;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrParameterListOwner;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;

public interface GrFunctionalExpression extends GrExpression, GrParameterListOwner
{
	@Nonnull
	GrParameter [] getAllParameters();

	@Nullable
	PsiType getOwnerType();

	@Nullable
	PsiType getReturnType();

	@Nullable
	PsiElement getArrow();
}
