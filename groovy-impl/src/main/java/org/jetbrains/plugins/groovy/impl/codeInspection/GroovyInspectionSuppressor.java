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
package org.jetbrains.plugins.groovy.impl.codeInspection;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.inspection.InspectionSuppressor;
import consulo.language.editor.inspection.SuppressQuickFix;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.jetbrains.plugins.groovy.GroovyLanguage;

@ExtensionImpl
public class GroovyInspectionSuppressor implements InspectionSuppressor {
  @Override
  public boolean isSuppressedFor(@Nonnull PsiElement element, @Nonnull String name) {
    return GroovySuppressableInspectionTool.getElementToolSuppressedIn(element, name) != null;
  }

  @Override
  public SuppressQuickFix[] getSuppressActions(@Nonnull PsiElement element, String toolShortName) {
    return GroovySuppressableInspectionTool.getSuppressActions(toolShortName);
  }

  @Nonnull
  @Override
  public Language getLanguage() {
    return GroovyLanguage.INSTANCE;
  }
}

