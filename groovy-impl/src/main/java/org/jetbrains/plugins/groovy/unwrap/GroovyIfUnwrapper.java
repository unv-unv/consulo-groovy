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
package org.jetbrains.plugins.groovy.unwrap;

import consulo.language.editor.CodeInsightBundle;
import consulo.language.psi.PsiElement;
import consulo.language.util.IncorrectOperationException;
import consulo.language.editor.CodeInsightBundle;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrIfStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;

public class GroovyIfUnwrapper extends GroovyUnwrapper {
  public GroovyIfUnwrapper() {
    super(CodeInsightBundle.message("unwrap.if"));
  }

  public boolean isApplicableTo(PsiElement e) {
    return e instanceof GrIfStatement && !isElseBlock(e);
  }

  @Override
  protected void doUnwrap(PsiElement element, Context context) throws IncorrectOperationException {
    GrStatement then = ((GrIfStatement)element).getThenBranch();
    context.extractFromBlockOrSingleStatement(then, element);

    context.delete(element);
  }
}
