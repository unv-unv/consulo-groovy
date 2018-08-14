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
import javax.annotation.Nullable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;

/**
 * @author Max Medvedev
 */
public interface GrSignature
{
	boolean isValid();

	@Nullable
	GrSignature curry(@Nonnull PsiType[] args, int position, @Nonnull PsiElement context);

	void accept(@Nonnull GrSignatureVisitor visitor);
}