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
package org.jetbrains.plugins.groovy.impl.extensions;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiFile;
import consulo.util.dataholder.Key;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import javax.annotation.Nonnull;

/**
 * @author Sergey Evdokimov
 */
@ExtensionImpl
public final class GroovyUnresolvedReferenceFilterByFile extends GroovyUnresolvedHighlightFilter {

  private static final Key<Boolean> KEY = Key.create("GroovyUnresolvedHighlightFileFilter");

  @Override
  public boolean isReject(@Nonnull GrReferenceExpression expression) {
    PsiFile file = expression.getContainingFile();

    Boolean cachedValue = file.getUserData(KEY);

    if (cachedValue != null) return cachedValue;

    boolean res = false;

    for (GroovyUnresolvedHighlightFileFilter filter : GroovyUnresolvedHighlightFileFilter.EP_NAME.getExtensionList()) {
      if (filter.isReject(file)) {
        res = true;
        break;
      }
    }

    file.putUserData(KEY, res);

    return res;
  }
}
