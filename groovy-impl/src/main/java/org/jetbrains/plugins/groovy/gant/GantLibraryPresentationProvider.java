/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.jetbrains.plugins.groovy.gant;

import java.io.File;

import javax.annotation.Nonnull;
import javax.swing.Icon;

import org.jetbrains.annotations.Nls;
import org.jetbrains.plugins.groovy.config.GroovyLibraryPresentationProviderBase;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.awt.TargetAWT;
import icons.JetgroovyIcons;

/**
 * @author nik
 */
public class GantLibraryPresentationProvider extends GroovyLibraryPresentationProviderBase {
  private static final LibraryKind GANT_KIND = LibraryKind.create("gant");

  public GantLibraryPresentationProvider() {
    super(GANT_KIND);
  }

  public boolean managesLibrary(final VirtualFile[] libraryFiles) {
    return GantUtils.isGantLibrary(libraryFiles);
  }

  @Nls
  public String getLibraryVersion(final VirtualFile[] libraryFiles) {
    return GantUtils.getGantVersion(GantUtils.getGantLibraryHome(libraryFiles));
  }

  @Nonnull
  public Icon getIcon() {
    return TargetAWT.to(JetgroovyIcons.Groovy.Gant_sdk);
  }

  @Override
  public boolean isSDKHome(@Nonnull VirtualFile file) {
    return GantUtils.isGantSdkHome(file);
  }

  @Nonnull
  @Override
  public String getSDKVersion(String path) {
    return GantUtils.getGantVersion(path);
  }

  @Nls
  @Nonnull
  @Override
  public String getLibraryCategoryName() {
    return "Gant";
  }

  @Override
  protected void fillLibrary(String path, LibraryEditor libraryEditor) {
    File srcRoot = new File(path + "/src/main");
    if (srcRoot.exists()) {
      libraryEditor.addRoot(VfsUtil.getUrlForLibraryRoot(srcRoot), OrderRootType.SOURCES);
    }

    // Add Gant jars
    File lib = new File(path + "/lib");
    File[] jars = lib.exists() ? lib.listFiles() : new File[0];
    if (jars != null) {
      for (File file : jars) {
        if (file.getName().endsWith(".jar")) {
          libraryEditor.addRoot(VfsUtil.getUrlForLibraryRoot(file), OrderRootType.CLASSES);
        }
      }
    }
  }
}
