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
package org.jetbrains.plugins.groovy.lang.psi;

import consulo.language.psi.PsiRecursiveVisitor;

/**
 * @author ven
 */
public abstract class GroovyRecursiveElementVisitor extends GroovyElementVisitor implements PsiRecursiveVisitor {

  @Override
  public void visitElement(GroovyPsiElement element) {
    element.acceptChildren(this);
  }
}
