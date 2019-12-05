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
package org.jetbrains.plugins.groovy.lang.psi.impl;

import com.intellij.openapi.util.Computable;
import consulo.util.dataholder.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.impl.source.resolve.ResolveCache;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.Function;
import javax.annotation.Nonnull;

import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.dataFlow.types.TypeInferenceHelper;

import java.util.Map;

import static com.intellij.util.containers.ContainerUtil.newHashMap;

/**
 * @author peter
 */
public interface InferenceContext {
  InferenceContext TOP_CONTEXT = new InferenceContext() {
    @javax.annotation.Nullable
    @Override
    public PsiType getVariableType(@Nonnull GrReferenceExpression ref) {
      return TypeInferenceHelper.getInferredType(ref);
    }

    @Override
    public <T> T getCachedValue(@Nonnull GroovyPsiElement element, @Nonnull final Computable<T> computable) {
      CachedValuesManager manager = CachedValuesManager.getManager(element.getProject());
      Key<CachedValue<T>> key = manager.getKeyForClass(computable.getClass());
      return manager.getCachedValue(element, key, new CachedValueProvider<T>() {
        @javax.annotation.Nullable
        @Override
        public Result<T> compute() {
          return Result.create(computable.compute(), PsiModificationTracker.MODIFICATION_COUNT);
        }
      }, false);
    }

    @Override
    public <T extends PsiPolyVariantReference> GroovyResolveResult[] multiResolve(@Nonnull T ref,
                                                                                  boolean incomplete,
                                                                                  ResolveCache.PolyVariantResolver<T> resolver) {
      ResolveResult[] results = ResolveCache.getInstance(ref.getElement().getProject()).resolveWithCaching(ref, resolver, true, incomplete);
      return results.length == 0 ? GroovyResolveResult.EMPTY_ARRAY : (GroovyResolveResult[])results;
    }

    @javax.annotation.Nullable
    @Override
    public <T extends GroovyPsiElement> PsiType getExpressionType(@Nonnull T element, @Nonnull Function<T, PsiType> calculator) {
      return GroovyPsiManager.getInstance(element.getProject()).getType(element, calculator);
    }
  };

  @javax.annotation.Nullable
  PsiType getVariableType(@Nonnull GrReferenceExpression ref);

  <T> T getCachedValue(@Nonnull GroovyPsiElement element, @Nonnull Computable<T> computable);

  <T extends PsiPolyVariantReference> GroovyResolveResult[] multiResolve(@Nonnull T ref, boolean incomplete, ResolveCache.PolyVariantResolver<T> resolver);

  @javax.annotation.Nullable
  <T extends GroovyPsiElement> PsiType getExpressionType(@Nonnull T element, @Nonnull Function<T, PsiType> calculator);

  class PartialContext implements InferenceContext {
    private final Map<String, PsiType> myTypes;
    private final Map<PsiElement, Map<Object, Object>> myCache = newHashMap();

    public PartialContext(@Nonnull Map<String, PsiType> types) {
      myTypes = types;
    }

    @javax.annotation.Nullable
    @Override
    public PsiType getVariableType(@Nonnull GrReferenceExpression ref) {
      return myTypes.get(ref.getReferenceName());
    }

    @Override
    public <T> T getCachedValue(@Nonnull GroovyPsiElement element, @Nonnull Computable<T> computable) {
      return _getCachedValue(element, computable, computable.getClass());
    }

    private <T> T _getCachedValue(@javax.annotation.Nullable PsiElement element, @Nonnull Computable<T> computable, @Nonnull Object key) {
      Map<Object, Object> map = myCache.get(element);
      if (map == null) {
        myCache.put(element, map = newHashMap());
      }
      if (map.containsKey(key)) {
        //noinspection unchecked
        return (T)map.get(key);
      }

      T result = computable.compute();
      map.put(key, result);
      return result;
    }

    @Nonnull
    @Override
    public <T extends PsiPolyVariantReference> GroovyResolveResult[] multiResolve(@Nonnull final T ref,
                                                                                  final boolean incomplete,
                                                                                  @Nonnull final ResolveCache.PolyVariantResolver<T> resolver) {
      return _getCachedValue(ref.getElement(), new Computable<GroovyResolveResult[]>() {
        @Override
        public GroovyResolveResult[] compute() {
          return (GroovyResolveResult[])resolver.resolve(ref, incomplete);
        }
      }, Pair.create(incomplete, resolver.getClass()));
    }

    @javax.annotation.Nullable
    @Override
    public <T extends GroovyPsiElement> PsiType getExpressionType(@Nonnull final T element, @Nonnull final Function<T, PsiType> calculator) {
      return _getCachedValue(element, new Computable<PsiType>() {
        @Override
        public PsiType compute() {
          PsiType type = calculator.fun(element);
          return type == PsiType.NULL ? null : type;
        }
      }, "type");
    }
  }

}
