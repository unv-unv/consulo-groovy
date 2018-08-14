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
package org.jetbrains.plugins.groovy.refactoring.introduce;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.introduce.inplace.InplaceVariableIntroducer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * @author Max Medvedev
 */
public abstract class GrInplaceIntroducer extends InplaceVariableIntroducer<PsiElement> {
  public GrInplaceIntroducer(@Nonnull GrVariable elementToRename,
                             @Nonnull Editor editor,
                             @Nonnull Project project,
                             @Nonnull String title,
                             @Nonnull List<RangeMarker> occurrences,
                             @Nullable PsiElement elementToIntroduce) {
    super(elementToRename, editor, project, title, PsiElement.EMPTY_ARRAY, elementToIntroduce);
    setOccurrenceMarkers(occurrences);
  }

  @javax.annotation.Nullable
  @Override
  protected PsiElement getNameIdentifier() {
    return getVariable().getNameIdentifierGroovy();
  }

  @Nullable
  @Override
  protected GrVariable getVariable() {
    return (GrVariable)super.getVariable();
  }

  public abstract LinkedHashSet<String> suggestNames(GrIntroduceContext context);
}