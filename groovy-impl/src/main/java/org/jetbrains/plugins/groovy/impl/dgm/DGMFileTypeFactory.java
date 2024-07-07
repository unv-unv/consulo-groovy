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
package org.jetbrains.plugins.groovy.impl.dgm;

import com.intellij.lang.properties.PropertiesFileType;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.SystemInfo;
import consulo.virtualFileSystem.fileType.FileNameMatcher;
import consulo.virtualFileSystem.fileType.FileNameMatcherFactory;
import consulo.virtualFileSystem.fileType.FileTypeConsumer;
import consulo.virtualFileSystem.fileType.FileTypeFactory;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;

/**
 * @author Max Medvedev
 */
@ExtensionImpl
public class DGMFileTypeFactory extends FileTypeFactory {
  private final FileNameMatcherFactory myFileNameMatcherFactory;

  @Inject
  public DGMFileTypeFactory(FileNameMatcherFactory fileNameMatcherFactory) {
    myFileNameMatcherFactory = fileNameMatcherFactory;
  }

  @Override
  public void createFileTypes(@Nonnull FileTypeConsumer consumer) {
    FileNameMatcher matcher =
      myFileNameMatcherFactory.createExactFileNameMatcher(GroovyExtensionProvider.ORG_CODEHAUS_GROOVY_RUNTIME_EXTENSION_MODULE,
                                                          !SystemInfo.isFileSystemCaseSensitive);
    consumer.consume(PropertiesFileType.INSTANCE, matcher);
  }
}
