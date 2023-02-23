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
package org.jetbrains.plugins.groovy.intentions.closure;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElement;
import org.jetbrains.plugins.groovy.intentions.base.ErrorUtil;
import org.jetbrains.plugins.groovy.intentions.base.PsiElementPredicate;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;

class SingleArgClosurePredicate implements PsiElementPredicate {

    public boolean satisfiedBy(PsiElement element) {
        if (!(element instanceof GrClosableBlock)) {
            return false;
        }
        final GrClosableBlock closure = (GrClosableBlock) element;
        if(closure.getParameterList().getParametersCount() !=1)
        {
            return false;
        }
        return !ErrorUtil.containsError(element);
    }
}
