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
package org.jetbrains.plugins.groovy.lang.psi.api.signatures;

import javax.annotation.Nonnull;

import org.jetbrains.plugins.groovy.lang.psi.api.types.GrClosureParameter;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;

/**
 * @author Maxim.Medvedev
 */
public interface GrClosureSignature extends GrSignature {
	GrClosureSignature[] EMPTY_ARRAY = new GrClosureSignature[0];

	@Nonnull
	PsiSubstitutor getSubstitutor();

	@Nonnull
	GrClosureParameter[] getParameters();

	int getParameterCount();

	boolean isVarargs();

	@javax.annotation.Nullable
	PsiType getReturnType();

	boolean isCurried();
}
