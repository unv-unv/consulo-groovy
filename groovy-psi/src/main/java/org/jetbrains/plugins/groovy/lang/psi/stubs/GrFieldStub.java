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
package org.jetbrains.plugins.groovy.lang.psi.stubs;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrEnumConstant;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.NamedStub;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.io.StringRef;

/**
 * @author ilyas
 */
public class GrFieldStub extends StubBase<GrField> implements NamedStub<GrField> {
  public static final byte IS_PROPERTY = 0x01;
  public static final byte IS_ENUM_CONSTANT = 0x02;
  public static final byte IS_DEPRECATED_BY_DOC_TAG = 0x04;

  private final byte myFlags;
  private final StringRef myName;
  private final String[] myAnnotations;
  private final String[] myNamedParameters;
  private final String myTypeText;

  public GrFieldStub(StubElement parent,
                         StringRef name,
                         final String[] annotations,
                         String[] namedParameters,
                         final IStubElementType elemType,
                         byte flags, @javax.annotation.Nullable String typeText) {
    super(parent, elemType);
    myName = name;
    myAnnotations = annotations;
    myNamedParameters = namedParameters;
    myFlags = flags;
    myTypeText = typeText;
  }

  @Nonnull
  public String getName() {
    return StringRef.toString(myName);
  }

  public String[] getAnnotations() {
    return myAnnotations;
  }

  @Nonnull
  public String[] getNamedParameters() {
    return myNamedParameters;
  }

  public boolean isProperty() {
    return (myFlags & IS_PROPERTY) != 0;
  }

  public boolean isDeprecatedByDocTag() {
    return (myFlags & IS_DEPRECATED_BY_DOC_TAG) != 0;
  }

  public byte getFlags() {
    return myFlags;
  }

  @Nullable
  public String getTypeText() {
    return myTypeText;
  }

  public static byte buildFlags(GrField field) {
    byte f = 0;
    if (field instanceof GrEnumConstant) {
      f |= IS_ENUM_CONSTANT;
    }

    if (field.isProperty()) {
      f |= IS_PROPERTY;
    }

    if (PsiImplUtil.isDeprecatedByDocTag(field)) {
      f|= IS_DEPRECATED_BY_DOC_TAG;
    }
    return f;
  }

  public static boolean isEnumConstant(byte flags) {
    return (flags & IS_ENUM_CONSTANT) != 0;
  }
}
