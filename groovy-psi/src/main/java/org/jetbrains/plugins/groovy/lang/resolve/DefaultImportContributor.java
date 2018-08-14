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
package org.jetbrains.plugins.groovy.lang.resolve;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import com.intellij.openapi.extensions.ExtensionPointName;

/**
 * @author peter
 */
public abstract class DefaultImportContributor {
  public static final ExtensionPointName<DefaultImportContributor> EP_NAME = ExtensionPointName.create("org.intellij.groovy.defaultImportContributor");
  
  public List<String> appendImplicitlyImportedPackages(@Nonnull GroovyFile file) {
    return Collections.emptyList();
  }
  
}