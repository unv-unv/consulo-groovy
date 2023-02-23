/*
 * Copyright 2007-2008 Dave Griffith
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
package org.jetbrains.plugins.groovy.codeInspection.confusing;

import javax.annotation.Nonnull;

import consulo.language.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nullable;
import org.jetbrains.plugins.groovy.codeInspection.BaseInspection;
import org.jetbrains.plugins.groovy.codeInspection.BaseInspectionVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrConditionalExpression;

public class GroovyNestedConditionalInspection extends BaseInspection {

  @Nls
  @Nonnull
  public String getGroupDisplayName() {
    return CONFUSING_CODE_CONSTRUCTS;
  }

  @Nls
  @Nonnull
  public String getDisplayName() {
    return "Nested conditional expression";
  }

  @Nullable
  protected String buildErrorString(Object... args) {
    return "Nested conditional expression #loc";

  }

  public BaseInspectionVisitor buildVisitor() {
    return new Visitor();
  }

  private static class Visitor extends BaseInspectionVisitor {

    public void visitConditionalExpression(GrConditionalExpression grConditionalExpression) {
      super.visitConditionalExpression(grConditionalExpression);
      final GrConditionalExpression containingConditional =
          PsiTreeUtil.getParentOfType(grConditionalExpression, GrConditionalExpression.class);
      if (containingConditional == null) {
        return;
      }
      registerError(grConditionalExpression);
    }
  }
}