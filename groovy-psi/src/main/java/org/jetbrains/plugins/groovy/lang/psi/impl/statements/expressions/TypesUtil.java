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

import gnu.trove.THashMap;
import gnu.trove.TIntObjectHashMap;
import gnu.trove.TObjectIntHashMap;

import java.util.Iterator;
import java.util.Map;

import javax.annotation.Nonnull;

import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.parser.GroovyElementTypes;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.SpreadState;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotation;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationArrayInitializer;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationMemberValue;
import org.jetbrains.plugins.groovy.lang.psi.api.signatures.GrClosureSignature;
import org.jetbrains.plugins.groovy.lang.psi.api.signatures.GrSignature;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrClosureType;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrImmediateTupleType;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrMapType;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrTraitType;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrTupleType;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiManager;
import org.jetbrains.plugins.groovy.lang.psi.impl.LazyFqnClassType;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.signatures.GrImmediateClosureSignatureImpl;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.GrTypeConverter;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.GrTypeConverter.ApplicableTo;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiSubstitutorImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.psi.util.TypeConversionUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ComparatorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashMap;

/**
 * @author ven
 */
public class TypesUtil {

  @NonNls
  public static final Map<String, PsiType> ourQNameToUnboxed = new HashMap<String, PsiType>();
  public static final PsiPrimitiveType[] PRIMITIVES = {
    PsiType.BYTE,
    PsiType.CHAR,
    PsiType.DOUBLE,
    PsiType.FLOAT,
    PsiType.INT,
    PsiType.SHORT,
    PsiType.LONG,
    PsiType.BOOLEAN,
    PsiType.VOID
  };

  private TypesUtil() {
  }

  @Nonnull
  public static GroovyResolveResult[] getOverloadedOperatorCandidates(@Nonnull PsiType thisType,
                                                                      IElementType tokenType,
                                                                      @Nonnull GroovyPsiElement place,
                                                                      PsiType[] argumentTypes) {
    return getOverloadedOperatorCandidates(thisType, tokenType, place, argumentTypes, false);
  }

  @Nonnull
  public static GroovyResolveResult[] getOverloadedOperatorCandidates(@Nonnull PsiType thisType,
                                                                      IElementType tokenType,
                                                                      @Nonnull GroovyPsiElement place,
                                                                      PsiType[] argumentTypes,
                                                                      boolean incompleteCode) {
    return ResolveUtil.getMethodCandidates(thisType, ourOperationsToOperatorNames.get(tokenType), place, true, incompleteCode, false, argumentTypes);
  }


  public static GroovyResolveResult[] getOverloadedUnaryOperatorCandidates(@Nonnull PsiType thisType,
                                                                           IElementType tokenType,
                                                                           @Nonnull GroovyPsiElement place,
                                                                           PsiType[] argumentTypes) {
    return ResolveUtil.getMethodCandidates(thisType, ourUnaryOperationsToOperatorNames.get(tokenType), place, argumentTypes);
  }

  private static final Map<IElementType, String> ourPrimitiveTypesToClassNames = new HashMap<IElementType, String>();
  private static final String NULL = "null";

  static {
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mSTRING_LITERAL, CommonClassNames.JAVA_LANG_STRING);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mGSTRING_LITERAL, CommonClassNames.JAVA_LANG_STRING);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mREGEX_LITERAL, CommonClassNames.JAVA_LANG_STRING);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mDOLLAR_SLASH_REGEX_LITERAL, CommonClassNames.JAVA_LANG_STRING);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mNUM_INT, CommonClassNames.JAVA_LANG_INTEGER);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mNUM_LONG, CommonClassNames.JAVA_LANG_LONG);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mNUM_FLOAT, CommonClassNames.JAVA_LANG_FLOAT);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mNUM_DOUBLE, CommonClassNames.JAVA_LANG_DOUBLE);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mNUM_BIG_INT, GroovyCommonClassNames.JAVA_MATH_BIG_INTEGER);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.mNUM_BIG_DECIMAL, GroovyCommonClassNames.JAVA_MATH_BIG_DECIMAL);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kFALSE, CommonClassNames.JAVA_LANG_BOOLEAN);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kTRUE, CommonClassNames.JAVA_LANG_BOOLEAN);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kNULL, NULL);

    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kINT, CommonClassNames.JAVA_LANG_INTEGER);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kLONG, CommonClassNames.JAVA_LANG_LONG);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kFLOAT, CommonClassNames.JAVA_LANG_FLOAT);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kDOUBLE, CommonClassNames.JAVA_LANG_DOUBLE);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kBOOLEAN, CommonClassNames.JAVA_LANG_BOOLEAN);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kCHAR, CommonClassNames.JAVA_LANG_CHARACTER);
    ourPrimitiveTypesToClassNames.put(GroovyTokenTypes.kBYTE, CommonClassNames.JAVA_LANG_BYTE);
  }

  private static final Map<IElementType, String> ourOperationsToOperatorNames = new HashMap<IElementType, String>();
  private static final Map<IElementType, String> ourUnaryOperationsToOperatorNames = new HashMap<IElementType, String>();

  static {
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mPLUS, "plus");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mMINUS, "minus");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mBAND, "and");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mBOR, "or");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mBXOR, "xor");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mDIV, "div");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mMOD, "mod");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mSTAR, "multiply");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.kAS, "asType");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mCOMPARE_TO, "compareTo");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mGT, "compareTo");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mGE, "compareTo");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mLT, "compareTo");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mLE, "compareTo");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mSTAR_STAR, "power");
    ourOperationsToOperatorNames.put(GroovyElementTypes.COMPOSITE_LSHIFT_SIGN, "leftShift");
    ourOperationsToOperatorNames.put(GroovyElementTypes.COMPOSITE_RSHIFT_SIGN, "rightShift");
    ourOperationsToOperatorNames.put(GroovyElementTypes.COMPOSITE_TRIPLE_SHIFT_SIGN, "rightShiftUnsigned");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mEQUAL, "equals");
    ourOperationsToOperatorNames.put(GroovyTokenTypes.mNOT_EQUAL, "equals");

    ourUnaryOperationsToOperatorNames.put(GroovyTokenTypes.mLNOT, "asBoolean");
    ourUnaryOperationsToOperatorNames.put(GroovyTokenTypes.mPLUS, "positive");
    ourUnaryOperationsToOperatorNames.put(GroovyTokenTypes.mMINUS, "negative");
    ourUnaryOperationsToOperatorNames.put(GroovyTokenTypes.mDEC, "previous");
    ourUnaryOperationsToOperatorNames.put(GroovyTokenTypes.mINC, "next");
    ourUnaryOperationsToOperatorNames.put(GroovyTokenTypes.mBNOT, "bitwiseNegate");
  }

  private static final TObjectIntHashMap<String> TYPE_TO_RANK = new TObjectIntHashMap<String>();

  static {
    TYPE_TO_RANK.put(CommonClassNames.JAVA_LANG_BYTE, 1);
    TYPE_TO_RANK.put(CommonClassNames.JAVA_LANG_SHORT, 2);
    TYPE_TO_RANK.put(CommonClassNames.JAVA_LANG_INTEGER, 3);
    TYPE_TO_RANK.put(CommonClassNames.JAVA_LANG_LONG, 4);
    TYPE_TO_RANK.put(GroovyCommonClassNames.JAVA_MATH_BIG_INTEGER, 5);
    TYPE_TO_RANK.put(GroovyCommonClassNames.JAVA_MATH_BIG_DECIMAL, 6);
    TYPE_TO_RANK.put(CommonClassNames.JAVA_LANG_FLOAT, 7);
    TYPE_TO_RANK.put(CommonClassNames.JAVA_LANG_DOUBLE, 8);
    TYPE_TO_RANK.put(CommonClassNames.JAVA_LANG_NUMBER, 9);
  }

  static {
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_BOOLEAN, PsiType.BOOLEAN);
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_BYTE, PsiType.BYTE);
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_CHARACTER, PsiType.CHAR);
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_SHORT, PsiType.SHORT);
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_INTEGER, PsiType.INT);
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_LONG, PsiType.LONG);
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_FLOAT, PsiType.FLOAT);
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_DOUBLE, PsiType.DOUBLE);
    ourQNameToUnboxed.put(CommonClassNames.JAVA_LANG_VOID, PsiType.VOID);
  }


  private static final TIntObjectHashMap<String> RANK_TO_TYPE = new TIntObjectHashMap<String>();

  static {
    RANK_TO_TYPE.put(1, CommonClassNames.JAVA_LANG_INTEGER);
    RANK_TO_TYPE.put(2, CommonClassNames.JAVA_LANG_INTEGER);
    RANK_TO_TYPE.put(3, CommonClassNames.JAVA_LANG_INTEGER);
    RANK_TO_TYPE.put(4, CommonClassNames.JAVA_LANG_LONG);
    RANK_TO_TYPE.put(5, GroovyCommonClassNames.JAVA_MATH_BIG_INTEGER);
    RANK_TO_TYPE.put(6, GroovyCommonClassNames.JAVA_MATH_BIG_DECIMAL);
    RANK_TO_TYPE.put(7, CommonClassNames.JAVA_LANG_DOUBLE);
    RANK_TO_TYPE.put(8, CommonClassNames.JAVA_LANG_DOUBLE);
    RANK_TO_TYPE.put(9, CommonClassNames.JAVA_LANG_NUMBER);
  }

  /**
   * @deprecated see {@link #canAssign}
   */
  @Deprecated
  public static boolean isAssignable(@Nullable PsiType lType, @Nullable PsiType rType, @Nonnull PsiElement context) {
    if (lType == null || rType == null) {
      return false;
    }
    return canAssign(lType, rType, context, ApplicableTo.ASSIGNMENT) == ConversionResult.OK;
  }

  @Nonnull
  public static ConversionResult canAssign(@Nonnull PsiType targetType,
                                           @Nonnull PsiType actualType,
                                           @Nonnull PsiElement context,
                                           @Nonnull ApplicableTo position) {
    if (actualType instanceof PsiIntersectionType) {
      ConversionResult min = ConversionResult.ERROR;
      for (PsiType child : ((PsiIntersectionType)actualType).getConjuncts()) {
        final ConversionResult result = canAssign(targetType, child, context, position);
        if (result.ordinal() < min.ordinal()) {
          min = result;
        }
        if (min == ConversionResult.OK) {
          return ConversionResult.OK;
        }
      }
      return min;
    }
    
    if (targetType instanceof PsiIntersectionType) {
      ConversionResult max = ConversionResult.OK;
      for (PsiType child : ((PsiIntersectionType)targetType).getConjuncts()) {
        final ConversionResult result = canAssign(child, actualType, context, position);
        if (result.ordinal() > max.ordinal()) {
          max = result;
        }
        if (max == ConversionResult.ERROR) {
          return ConversionResult.ERROR;
        }
      }
      return max;
    }

    final ConversionResult result = areTypesConvertible(targetType, actualType, context, position);
    if (result != null) return result;

    if (isAssignableWithoutConversions(targetType, actualType, context)) {
      return ConversionResult.OK;
    }

    final PsiManager manager = context.getManager();
    final GlobalSearchScope scope = context.getResolveScope();
    targetType = boxPrimitiveType(targetType, manager, scope);
    actualType = boxPrimitiveType(actualType, manager, scope);

    if (targetType.isAssignableFrom(actualType)) {
      return ConversionResult.OK;
    }

    return ConversionResult.ERROR;
  }

  public static boolean isAssignableByMethodCallConversion(@Nullable PsiType targetType,
                                                           @Nullable PsiType actualType,
                                                           @Nonnull PsiElement context) {

    if (targetType == null || actualType == null) return false;
    return canAssign(targetType, actualType, context, ApplicableTo.METHOD_PARAMETER) == ConversionResult.OK;
  }

  @Nullable
  private static ConversionResult areTypesConvertible(@Nonnull PsiType targetType,
                                                      @Nonnull PsiType actualType,
                                                      @Nonnull PsiElement context,
                                                      @Nonnull ApplicableTo position) {
    if (!(context instanceof GroovyPsiElement)) return null;
    for (GrTypeConverter converter : GrTypeConverter.EP_NAME.getExtensions()) {
      if (!converter.isApplicableTo(position)) continue;
      final ConversionResult result = converter.isConvertibleEx(targetType, actualType, (GroovyPsiElement)context, position);
      if (result != null) return result;
    }
    return null;
  }

  public static boolean isAssignableWithoutConversions(@Nullable PsiType lType,
                                                       @Nullable PsiType rType,
                                                       @Nonnull PsiElement context) {
    if (lType == null || rType == null) return false;

    if (rType == PsiType.NULL) {
      return !(lType instanceof PsiPrimitiveType);
    }

    PsiManager manager = context.getManager();
    GlobalSearchScope scope = context.getResolveScope();

    if (rType instanceof GrTupleType && ((GrTupleType)rType).getComponentTypes().length == 0) {
      if (lType instanceof PsiArrayType ||
          InheritanceUtil.isInheritor(lType, CommonClassNames.JAVA_UTIL_LIST) ||
          InheritanceUtil.isInheritor(lType, CommonClassNames.JAVA_UTIL_SET)) {
        return true;
      }
    }

    if (rType instanceof GrTraitType) {
      if (isAssignableWithoutConversions(lType, ((GrTraitType)rType).getExprType(), context)) return true;
      for (PsiClassType trait : ((GrTraitType)rType).getTraitTypes()) {
        if (isAssignableWithoutConversions(lType, trait, context)) return true;
      }
      return false;
    }

    if (isClassType(rType, GroovyCommonClassNames.GROOVY_LANG_GSTRING) && lType.equalsToText(CommonClassNames.JAVA_LANG_STRING)) {
      return true;
    }

    if (isNumericType(lType) && isNumericType(rType)) {
      lType = unboxPrimitiveTypeWrapper(lType);
      if (isClassType(lType, GroovyCommonClassNames.JAVA_MATH_BIG_DECIMAL)) lType = PsiType.DOUBLE;
      rType = unboxPrimitiveTypeWrapper(rType);
      if (isClassType(rType, GroovyCommonClassNames.JAVA_MATH_BIG_DECIMAL)) rType = PsiType.DOUBLE;
    }
    else {
      rType = boxPrimitiveType(rType, manager, scope);
      lType = boxPrimitiveType(lType, manager, scope);
    }

    if (rType instanceof GrMapType || rType instanceof GrTupleType) {
      Boolean result = isAssignableForNativeTypes(lType, (PsiClassType)rType, context);
      if (result != null && result.booleanValue()) return true;
    }

    if (rType instanceof GrClosureType) {
      if (canMakeClosureRaw(lType)) {
        rType = ((GrClosureType)rType).rawType();
      }
    }

    return TypeConversionUtil.isAssignable(lType, rType);
  }

  private static boolean canMakeClosureRaw(PsiType type) {
    if (!(type instanceof PsiClassType)) return true;

    final PsiType[] parameters = ((PsiClassType)type).getParameters();

    if (parameters.length != 1) return true;

    final PsiType parameter = parameters[0];
    if (parameter instanceof PsiWildcardType) return true;

    return false;
  }

  @javax.annotation.Nullable
  private static Boolean isAssignableForNativeTypes(@Nonnull PsiType lType,
                                                    @Nonnull PsiClassType rType,
                                                    @Nonnull PsiElement context) {
    if (!(lType instanceof PsiClassType)) return null;
    final PsiClassType.ClassResolveResult leftResult = ((PsiClassType)lType).resolveGenerics();
    final PsiClassType.ClassResolveResult rightResult = rType.resolveGenerics();
    final PsiClass leftClass = leftResult.getElement();
    PsiClass rightClass = rightResult.getElement();
    if (rightClass == null || leftClass == null) return null;

    if (!InheritanceUtil.isInheritorOrSelf(rightClass, leftClass, true)) return Boolean.FALSE;

    PsiSubstitutor rightSubstitutor = rightResult.getSubstitutor();

    if (!leftClass.hasTypeParameters()) return Boolean.TRUE;
    PsiSubstitutor leftSubstitutor = leftResult.getSubstitutor();

    if (!leftClass.getManager().areElementsEquivalent(leftClass, rightClass)) {
      rightSubstitutor = TypeConversionUtil.getSuperClassSubstitutor(leftClass, rightClass, rightSubstitutor);
      rightClass = leftClass;
    }
    else if (!rightClass.hasTypeParameters()) return Boolean.TRUE;

    Iterator<PsiTypeParameter> li = PsiUtil.typeParametersIterator(leftClass);
    Iterator<PsiTypeParameter> ri = PsiUtil.typeParametersIterator(rightClass);
    while (li.hasNext()) {
      if (!ri.hasNext()) return Boolean.FALSE;
      PsiTypeParameter lp = li.next();
      PsiTypeParameter rp = ri.next();
      final PsiType typeLeft = leftSubstitutor.substitute(lp);
      if (typeLeft == null) continue;
      final PsiType typeRight = rightSubstitutor.substituteWithBoundsPromotion(rp);
      if (typeRight == null) {
        return Boolean.TRUE;
      }
      if (!isAssignableWithoutConversions(typeLeft, typeRight, context)) return Boolean.FALSE;
    }
    return Boolean.TRUE;
  }

  @Nonnull
  public static ConversionResult canCast(@Nonnull PsiType targetType, @Nonnull PsiType actualType, @Nonnull PsiElement context) {
    final ConversionResult result = areTypesConvertible(targetType, actualType, context, ApplicableTo.EXPLICIT_CAST);
    if (result != null) return result;
    return TypeConversionUtil.areTypesConvertible(targetType, actualType) ? ConversionResult.OK : ConversionResult.ERROR;
  }

  @Nonnull
  public static ConversionResult canAssignWithinMultipleAssignment(@Nonnull PsiType targetType,
                                                                   @Nonnull PsiType actualType,
                                                                   @Nonnull PsiElement context) {
    return isAssignableWithoutConversions(targetType, actualType, context) ? ConversionResult.OK : ConversionResult.ERROR;
  }

  public static boolean isNumericType(@Nullable PsiType type) {
    if (type instanceof PsiClassType) {
      return TYPE_TO_RANK.contains(getQualifiedName(type));
    }

    return type instanceof PsiPrimitiveType && TypeConversionUtil.isNumericType(type);
  }

  public static PsiType unboxPrimitiveTypeWrapperAndEraseGenerics(PsiType result) {
    return TypeConversionUtil.erasure(unboxPrimitiveTypeWrapper(result));
  }

  public static PsiType unboxPrimitiveTypeWrapper(@Nullable PsiType type) {
    if (type instanceof PsiClassType) {
      final PsiClass psiClass = ((PsiClassType)type).resolve();
      if (psiClass != null) {
        PsiType unboxed = ourQNameToUnboxed.get(psiClass.getQualifiedName());
        if (unboxed != null) type = unboxed;
      }
    }
    return type;
  }

  public static PsiType boxPrimitiveType(@Nullable PsiType result,
                                         @Nonnull PsiManager manager,
                                         @Nonnull GlobalSearchScope resolveScope,
                                         boolean boxVoid) {
    if (result instanceof PsiPrimitiveType && (boxVoid || result != PsiType.VOID)) {
      PsiPrimitiveType primitive = (PsiPrimitiveType)result;
      String boxedTypeName = primitive.getBoxedTypeName();
      if (boxedTypeName != null) {
        return GroovyPsiManager.getInstance(manager.getProject()).createTypeByFQClassName(boxedTypeName, resolveScope);
      }
    }

    return result;
  }

  public static PsiType boxPrimitiveType(@Nullable PsiType result, @Nonnull PsiManager manager, @Nonnull GlobalSearchScope resolveScope) {
    return boxPrimitiveType(result, manager, resolveScope, false);
  }

  @Nonnull
  public static PsiClassType createType(String fqName, @Nonnull PsiElement context) {
    return createTypeByFQClassName(fqName, context);
  }

  @Nonnull
  public static PsiClassType getJavaLangObject(@Nonnull PsiElement context) {
    return LazyFqnClassType.getLazyType(CommonClassNames.JAVA_LANG_OBJECT, context);
  }

  @Nullable
  public static PsiType getLeastUpperBoundNullable(@Nullable PsiType type1, @Nullable PsiType type2, @Nonnull PsiManager manager) {
    if (type1 == null) return type2;
    if (type2 == null) return type1;
    return getLeastUpperBound(type1, type2, manager);
  }

  @javax.annotation.Nullable
  public static PsiType getLeastUpperBoundNullable(@Nonnull Iterable<PsiType> collection, @Nonnull PsiManager manager) {
    Iterator<PsiType> iterator = collection.iterator();
    if (!iterator.hasNext()) return null;
    PsiType result = iterator.next();
    while (iterator.hasNext()) {
      result = getLeastUpperBoundNullable(result, iterator.next(), manager);
    }
    return result;
  }

  @Nullable
  public static PsiType getLeastUpperBound(@Nonnull PsiType type1, @Nonnull PsiType type2, PsiManager manager) {
    if (type1 instanceof GrTupleType && type2 instanceof GrTupleType) {
      GrTupleType tuple1 = (GrTupleType)type1;
      GrTupleType tuple2 = (GrTupleType)type2;
      PsiType[] components1 = tuple1.getComponentTypes();
      PsiType[] components2 = tuple2.getComponentTypes();

      if (components1.length == 0) return genNewListBy(type2, manager);
      if (components2.length == 0) return genNewListBy(type1, manager);

      PsiType[] components3 = PsiType.createArray(Math.min(components1.length, components2.length));
      for (int i = 0; i < components3.length; i++) {
        PsiType c1 = components1[i];
        PsiType c2 = components2[i];
        if (c1 == null || c2 == null) {
          components3[i] = null;
        }
        else {
          components3[i] = getLeastUpperBound(c1, c2, manager);
        }
      }
      return new GrImmediateTupleType(components3, JavaPsiFacade.getInstance(manager.getProject()), tuple1.getScope().intersectWith(tuple2.getResolveScope()));
    }
    else if (checkEmptyListAndList(type1, type2)) {
      return genNewListBy(type2, manager);
    }
    else if (checkEmptyListAndList(type2, type1)) {
      return genNewListBy(type1, manager);
    }
    else if (type1 instanceof GrMapType && type2 instanceof GrMapType) {
      return GrMapType.merge(((GrMapType)type1), ((GrMapType)type2));
    }
    else if (checkEmptyMapAndMap(type1, type2)) {
      return genNewMapBy(type2, manager);
    }
    else if (checkEmptyMapAndMap(type2, type1)) {
      return genNewMapBy(type1, manager);
    }
    else if (type1 instanceof GrClosureType && type2 instanceof GrClosureType) {
      GrClosureType clType1 = (GrClosureType)type1;
      GrClosureType clType2 = (GrClosureType)type2;
      GrSignature signature1 = clType1.getSignature();
      GrSignature signature2 = clType2.getSignature();

      if (signature1 instanceof GrClosureSignature && signature2 instanceof GrClosureSignature) {
        if (((GrClosureSignature)signature1).getParameterCount() == ((GrClosureSignature)signature2).getParameterCount()) {
          final GrClosureSignature signature = GrImmediateClosureSignatureImpl.getLeastUpperBound(((GrClosureSignature)signature1),
                                                                                                  ((GrClosureSignature)signature2), manager);
          if (signature != null) {
            GlobalSearchScope scope = clType1.getResolveScope().intersectWith(clType2.getResolveScope());
            final LanguageLevel languageLevel = ComparatorUtil.max(clType1.getLanguageLevel(), clType2.getLanguageLevel());
            return GrClosureType.create(signature, scope, JavaPsiFacade.getInstance(manager.getProject()), languageLevel, true);
          }
        }
      }
    }
    else if (GroovyCommonClassNames.GROOVY_LANG_GSTRING.equals(getQualifiedName(type1)) &&
             CommonClassNames.JAVA_LANG_STRING.equals(getQualifiedName(type2))) {
      return type2;
    }
    else if (GroovyCommonClassNames.GROOVY_LANG_GSTRING.equals(getQualifiedName(type2)) &&
             CommonClassNames.JAVA_LANG_STRING.equals(getQualifiedName(type1))) {
      return type1;
    }
    return GenericsUtil.getLeastUpperBound(type1, type2, manager);
  }

  private static boolean checkEmptyListAndList(PsiType type1, PsiType type2) {
    if (type1 instanceof GrTupleType) {
      PsiType[] types = ((GrTupleType)type1).getComponentTypes();
      if (types.length == 0 && InheritanceUtil.isInheritor(type2, CommonClassNames.JAVA_UTIL_LIST)) return true;
    }

    return false;
  }

  private static PsiType genNewListBy(PsiType genericOwner, PsiManager manager) {
    PsiClass list = JavaPsiFacade.getInstance(manager.getProject()).findClass(CommonClassNames.JAVA_UTIL_LIST, genericOwner.getResolveScope());
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(manager.getProject());
    if (list == null) return factory.createTypeFromText(CommonClassNames.JAVA_UTIL_LIST, null);
    return factory.createType(list, PsiUtil.extractIterableTypeParameter(genericOwner, false));
  }

  private static boolean checkEmptyMapAndMap(PsiType type1, PsiType type2) {
    if (type1 instanceof GrMapType) {
      if (((GrMapType)type1).isEmpty() && InheritanceUtil.isInheritor(type2, CommonClassNames.JAVA_UTIL_MAP)) return true;
    }

    return false;
  }

  private static PsiType genNewMapBy(PsiType genericOwner, PsiManager manager) {
    PsiClass map = JavaPsiFacade.getInstance(manager.getProject()).findClass(CommonClassNames.JAVA_UTIL_MAP, genericOwner.getResolveScope());
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(manager.getProject());
    if (map == null) return factory.createTypeFromText(CommonClassNames.JAVA_UTIL_MAP, null);

    final PsiType key = PsiUtil.substituteTypeParameter(genericOwner, CommonClassNames.JAVA_UTIL_MAP, 0, false);
    final PsiType value = PsiUtil.substituteTypeParameter(genericOwner, CommonClassNames.JAVA_UTIL_MAP, 1, false);
    return factory.createType(map, key, value);
  }

  @Nullable
  public static PsiType getPsiType(PsiElement context, IElementType elemType) {
    if (elemType == GroovyTokenTypes.kNULL) {
      return PsiType.NULL;
    }
    final String typeName = getBoxedTypeName(elemType);
    if (typeName != null) {
      return createTypeByFQClassName(typeName, context);
    }
    return null;
  }

  @Nullable
  public static String getBoxedTypeName(IElementType elemType) {
    return ourPrimitiveTypesToClassNames.get(elemType);
  }

  @Nonnull
  public static PsiType getLeastUpperBound(PsiClass[] classes, PsiManager manager) {
    PsiElementFactory factory = JavaPsiFacade.getElementFactory(manager.getProject());

    if (classes.length == 0) return factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_OBJECT);

    PsiType type = factory.createType(classes[0]);

    for (int i = 1; i < classes.length; i++) {
      PsiType t = getLeastUpperBound(type, factory.createType(classes[i]), manager);
      if (t != null) {
        type = t;
      }
    }

    return type;
  }

  public static boolean isClassType(@Nullable PsiType type, @Nonnull String qName) {
    return qName.equals(getQualifiedName(type));
  }

  public static PsiSubstitutor composeSubstitutors(PsiSubstitutor s1, PsiSubstitutor s2) {
    final Map<PsiTypeParameter, PsiType> map = s1.getSubstitutionMap();
    Map<PsiTypeParameter, PsiType> result = new THashMap<PsiTypeParameter, PsiType>(map.size());
    for (PsiTypeParameter parameter : map.keySet()) {
      result.put(parameter, s2.substitute(map.get(parameter)));
    }
    final Map<PsiTypeParameter, PsiType> map2 = s2.getSubstitutionMap();
    for (PsiTypeParameter parameter : map2.keySet()) {
      if (!result.containsKey(parameter)) {
        result.put(parameter, map2.get(parameter));
      }
    }
    return PsiSubstitutorImpl.createSubstitutor(result);
  }

  @Nonnull
  public static PsiClassType createTypeByFQClassName(@Nonnull String fqName, @Nonnull PsiElement context) {
    return GroovyPsiManager.getInstance(context.getProject()).createTypeByFQClassName(fqName, context.getResolveScope());
  }

  @Nullable
  public static PsiType createJavaLangClassType(@Nullable PsiType type,
                                                Project project,
                                                GlobalSearchScope resolveScope) {
    final JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    PsiType result = null;
    PsiClass javaLangClass = facade.findClass(CommonClassNames.JAVA_LANG_CLASS, resolveScope);
    if (javaLangClass != null) {
      PsiSubstitutor substitutor = PsiSubstitutor.EMPTY;
      final PsiTypeParameter[] typeParameters = javaLangClass.getTypeParameters();
      if (typeParameters.length == 1) {
        substitutor = substitutor.put(typeParameters[0], type);
      }
      result = facade.getElementFactory().createType(javaLangClass, substitutor);
    }
    return result;
  }

  @Nonnull
  public static PsiPrimitiveType getPrimitiveTypeByText(String typeText) {
    for (final PsiPrimitiveType primitive : PRIMITIVES) {
      if (PsiType.VOID.equals(primitive)) {
        return primitive;
      }
      if (primitive.getCanonicalText().equals(typeText)) {
        return primitive;
      }
    }

    assert false : "Unknown primitive type";
    return null;
  }

  @Nonnull
  public static PsiClassType createListType(@Nonnull PsiClass elements) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(elements.getProject());
    GlobalSearchScope resolveScope = elements.getResolveScope();
    PsiClass listClass = facade.findClass(CommonClassNames.JAVA_UTIL_LIST, resolveScope);
    if (listClass == null) {
      return facade.getElementFactory().createTypeByFQClassName(CommonClassNames.JAVA_UTIL_LIST, resolveScope);
    }
    return facade.getElementFactory().createType(listClass, facade.getElementFactory().createType(elements));
  }

  @Nonnull
  public static PsiType createSetType(@Nonnull PsiElement context, @Nonnull PsiType type) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(context.getProject());
    GlobalSearchScope resolveScope = context.getResolveScope();

    PsiClass setClass = facade.findClass(CommonClassNames.JAVA_UTIL_SET, resolveScope);
    if (setClass != null && setClass.getTypeParameters().length == 1) {
      return facade.getElementFactory().createType(setClass, type);
    }

    return facade.getElementFactory().createTypeByFQClassName(CommonClassNames.JAVA_UTIL_SET, resolveScope);
  }

  public static boolean isAnnotatedCheckHierarchyWithCache(@Nonnull PsiClass aClass, @Nonnull String annotationFQN) {
    Map<String, PsiClass> classMap = ClassUtil.getSuperClassesWithCache(aClass);

    for (PsiClass psiClass : classMap.values()) {
      PsiModifierList modifierList = psiClass.getModifierList();
      if (modifierList != null) {
        if (modifierList.findAnnotation(annotationFQN) != null) {
          return true;
        }
      }
    }

    return false;
  }

  @javax.annotation.Nullable
  public static PsiType substituteAndNormalizeType(@javax.annotation.Nullable PsiType type,
                                                   @Nonnull PsiSubstitutor substitutor,
                                                   @Nullable SpreadState state, @Nonnull GrExpression expression) {
    if (type == null) return null;
    type = substitutor.substitute(type);
    if (type == null) return null;
    type = PsiImplUtil.normalizeWildcardTypeByPosition(type, expression);
    type = SpreadState.apply(type, state, expression.getProject());
    return type;
  }

  @javax.annotation.Nullable
  public static PsiType getItemType(@Nullable PsiType containerType) {
    if (containerType == null) return null;

    if (containerType instanceof PsiArrayType) return ((PsiArrayType)containerType).getComponentType();
    return PsiUtil.extractIterableTypeParameter(containerType, false);
  }

  @Nullable
  public static PsiType inferAnnotationMemberValueType(final GrAnnotationMemberValue value) {
    if (value instanceof GrExpression) {
      return ((GrExpression)value).getType();
    }

    else if (value instanceof GrAnnotation) {
      final PsiElement resolved = ((GrAnnotation)value).getClassReference().resolve();
      if (resolved instanceof PsiClass) {
        return JavaPsiFacade.getElementFactory(value.getProject()).createType((PsiClass)resolved, PsiSubstitutor.EMPTY);
      }

      return null;
    }

    else if (value instanceof GrAnnotationArrayInitializer) {
      return getTupleByAnnotationArrayInitializer((GrAnnotationArrayInitializer)value);
    }

    return null;
  }

  public static PsiType getTupleByAnnotationArrayInitializer(final GrAnnotationArrayInitializer value) {
    return new GrTupleType(value.getResolveScope(), JavaPsiFacade.getInstance(value.getProject())) {
      @Nonnull
      @Override
      protected PsiType[] inferComponents() {
        final GrAnnotationMemberValue[] initializers = value.getInitializers();
        return ContainerUtil.map(initializers, new Function<GrAnnotationMemberValue, PsiType>() {
          @Override
          public PsiType fun(GrAnnotationMemberValue value) {
            return inferAnnotationMemberValueType(value);
          }
        }, PsiType.createArray(initializers.length));
      }

      @Override
      public boolean isValid() {
        return value.isValid();
      }
    };
  }

  public static boolean resolvesTo(PsiType type, String fqn) {
    if (type instanceof PsiClassType) {
      final PsiClass resolved = ((PsiClassType)type).resolve();
      return resolved != null && fqn.equals(resolved.getQualifiedName());
    }
    return false;
  }

  @Nullable
  public static PsiType rawSecondGeneric(PsiType type, Project project) {
    if (!(type instanceof PsiClassType)) return null;

    final PsiClassType.ClassResolveResult result = ((PsiClassType)type).resolveGenerics();
    final PsiClass element = result.getElement();
    if (element == null) return null;

    final PsiType[] parameters = ((PsiClassType)type).getParameters();

    boolean changed = false;
    for (int i = 0; i < parameters.length; i++) {
      PsiType parameter = parameters[i];
      if (parameter == null) continue;

      final Ref<PsiType> newParam = new Ref<PsiType>();
      parameter.accept(new PsiTypeVisitorEx<Object>() {
        @Nullable
        @Override
        public Object visitClassType(PsiClassType classType) {
          if (classType.getParameterCount() > 0) {
            newParam.set(classType.rawType());
          }
          return null;
        }

        @Nullable
        @Override
        public Object visitCapturedWildcardType(PsiCapturedWildcardType capturedWildcardType) {
          newParam.set(capturedWildcardType.getWildcard().getBound());
          return null;
        }

        @Nullable
        @Override
        public Object visitWildcardType(PsiWildcardType wildcardType) {
          newParam.set(wildcardType.getBound());
          return null;
        }
      });

      if (!newParam.isNull()) {
        changed = true;
        parameters[i] = newParam.get();
      }
    }
    if (!changed) return null;
    return JavaPsiFacade.getElementFactory(project).createType(element, parameters);
  }

  public static boolean isPsiClassTypeToClosure(PsiType type) {
    if (!(type instanceof PsiClassType)) return false;

    final PsiClass psiClass = ((PsiClassType)type).resolve();
    if (psiClass == null) return false;

    return GroovyCommonClassNames.GROOVY_LANG_CLOSURE.equals(psiClass.getQualifiedName());
  }

  @Nullable
  public static String getQualifiedName(@Nullable PsiType type) {
    if (type instanceof PsiClassType) {
      PsiClass resolved = ((PsiClassType)type).resolve();
      if (resolved instanceof PsiAnonymousClass) {
        return getQualifiedName(((PsiAnonymousClass)resolved).getBaseClassType());
      }
      if (resolved != null) {
        return resolved.getQualifiedName();
      }
      else {
        return PsiNameHelper.getQualifiedClassName(type.getCanonicalText(), true);
      }
    }

    return null;
  }

  public static boolean isEnum(PsiType type) {
    if (type instanceof PsiClassType) {
      final PsiClass resolved = ((PsiClassType)type).resolve();
      return resolved != null && resolved.isEnum();
    }
    return false;
  }
}
