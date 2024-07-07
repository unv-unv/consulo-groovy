/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.impl.lang.completion.weighers;

import com.intellij.java.impl.psi.util.proximity.ReferenceListWeigher;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrReferenceList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrCodeReferenceElement;

/**
 * @author peter
 */
@ExtensionImpl(id = "groovyReferenceListWeigher", order = "before openedInEditor")
public class GrReferenceListWeigher extends ReferenceListWeigher {
  private static final PsiElementPattern.Capture<PsiElement> INSIDE_REFERENCE_LIST =
    PlatformPatterns.psiElement().withParents(GrCodeReferenceElement.class, GrReferenceList.class);

  @Override
  protected Preference getPreferredCondition(@Nonnull PsiElement position) {
    if (INSIDE_REFERENCE_LIST.accepts(position)) {
      GrReferenceList list = (GrReferenceList)position.getParent().getParent();
      PsiElement parent = list.getParent();
      if (parent instanceof GrTypeDefinition) {
        GrTypeDefinition cls = (GrTypeDefinition)parent;
        if (cls.isInterface() && list == cls.getExtendsClause() || list == cls.getImplementsClause()) {
          return Preference.Interfaces;
        }
        if (list == cls.getExtendsClause()) {
          return Preference.Classes;
        }
      }
      if (parent instanceof GrMethod && ((GrMethod)parent).getThrowsList() == list) {
        return Preference.Exceptions;
      }
    }

    return null;
  }
}
