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

package org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions;

import com.intellij.java.language.psi.CommonClassNames;
import com.intellij.java.language.psi.PsiType;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrTupleExpression;

/**
 * @author ilyas
 */
public class GrTupleExpressionImpl extends GrExpressionImpl implements GrTupleExpression {
    @Override
    public String toString() {
        return "Tuple Assignment Expression";
    }

    public GrTupleExpressionImpl(@Nonnull ASTNode node) {
        super(node);
    }

    @Override
    public PsiType getType() {
        return getTypeByFQName(CommonClassNames.JAVA_UTIL_LIST);
    }

    @Override
    public int indexOf(@Nonnull PsiElement element) {
        GrExpression[] children = getExpressions();
        return ArrayUtil.find(children, element);
    }

    @Override
    @Nonnull
    public GrExpression[] getExpressions() {
        return findChildrenByClass(GrExpression.class);
    }

    @Override
    public void accept(GroovyElementVisitor visitor) {
        visitor.visitTupleExpression(this);
    }
}
