package org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrConstructorInvocation;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyResolveResultImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiSubstitutor;

/**
 * Created by Max Medvedev on 15/06/14
 */
public class GrThisReferenceResolver
{
	/**
	 * @return null if ref is not actually 'this' reference
	 */
	@Nullable
	public static GroovyResolveResult[] resolveThisExpression(@Nonnull GrReferenceExpression ref)
	{
		GrExpression qualifier = ref.getQualifier();

		if(qualifier == null)
		{
			final PsiElement parent = ref.getParent();
			if(parent instanceof GrConstructorInvocation)
			{
				return ((GrConstructorInvocation) parent).multiResolve(false);
			}
			else
			{
				PsiClass aClass = PsiUtil.getContextClass(ref);
				if(aClass != null)
				{
					return new GroovyResolveResultImpl[]{new GroovyResolveResultImpl(aClass, null, null, PsiSubstitutor.EMPTY, true, true)};
				}
			}
		}
		else if(qualifier instanceof GrReferenceExpression)
		{
			GroovyResolveResult result = ((GrReferenceExpression) qualifier).advancedResolve();
			PsiElement resolved = result.getElement();
			if(resolved instanceof PsiClass && PsiUtil.hasEnclosingInstanceInScope((PsiClass) resolved, ref, false))
			{
				return new GroovyResolveResult[]{result};
			}
		}

		return null;
	}
}
