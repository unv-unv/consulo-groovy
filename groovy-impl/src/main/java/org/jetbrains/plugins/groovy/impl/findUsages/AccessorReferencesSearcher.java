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

package org.jetbrains.plugins.groovy.impl.findUsages;

import com.intellij.java.indexing.search.searches.MethodReferencesSearch;
import com.intellij.java.language.psi.PsiMethod;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.search.ReferencesSearchQueryExecutor;
import consulo.language.psi.search.UsageSearchContext;
import consulo.project.util.query.QueryExecutorBase;
import jakarta.annotation.Nonnull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;

import java.util.function.Predicate;

/**
 * author ven
 */
@ExtensionImpl
public class AccessorReferencesSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>
    implements ReferencesSearchQueryExecutor {
    public AccessorReferencesSearcher() {
        super(true);
    }

    @Override
    public void processQuery(
        @Nonnull ReferencesSearch.SearchParameters queryParameters,
        @Nonnull Predicate<? super PsiReference> consumer
    ) {
        PsiElement element = queryParameters.getElementToSearch();
        if (element instanceof PsiMethod method) {
            String propertyName = GroovyPropertyUtils.getPropertyName(method);
            if (propertyName == null) {
                return;
            }

            queryParameters.getOptimizer().searchWord(
                propertyName,
                GroovyScopeUtil.restrictScopeToGroovyFiles(queryParameters.getEffectiveSearchScope()),
                UsageSearchContext.IN_CODE,
                true,
                element
            );
        }
        else if (element instanceof GrField field) {
            for (GrAccessorMethod method : field.getGetters()) {
                MethodReferencesSearch.search(method, queryParameters.getEffectiveSearchScope(), true).forEach(consumer);
            }

            GrAccessorMethod setter = field.getSetter();
            if (setter != null) {
                MethodReferencesSearch.search(setter, queryParameters.getEffectiveSearchScope(), true).forEach(consumer);
            }
        }
    }
}
