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

package org.jetbrains.plugins.groovy.annotator.intentions.dynamic;

import javax.annotation.Nonnull;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.jetbrains.annotations.NonNls;

import javax.annotation.Nullable;
import org.jetbrains.plugins.groovy.annotator.intentions.dynamic.elements.DClassElement;
import org.jetbrains.plugins.groovy.annotator.intentions.dynamic.elements.DPropertyElement;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrDynamicImplicitElement;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrImplicitVariableImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ui.tree.TreeUtil;
import icons.JetgroovyIcons;

/**
 * @author ilyas
 */
public class GrDynamicImplicitProperty extends GrImplicitVariableImpl implements GrDynamicImplicitElement, PsiField
{
	private final String myContainingClassName;
	private final Project myProject;

	public GrDynamicImplicitProperty(PsiManager manager,
			@NonNls String name,
			@NonNls @Nonnull String type,
			String containingClassName)
	{
		super(manager, name, type, null);
		myContainingClassName = containingClassName;
		myProject = manager.getProject();
		setOriginInfo("dynamic property");
	}

	@Override
	@Nullable
	public PsiClass getContainingClassElement()
	{
		final PsiClassType containingClassType = JavaPsiFacade.getInstance(getProject()).getElementFactory().
				createTypeByFQClassName(myContainingClassName, ProjectScope.getAllScope(getProject()));

		return containingClassType.resolve();
	}

	@Override
	public String getContainingClassName()
	{
		return myContainingClassName;
	}

	@Override
	public PsiFile getContainingFile()
	{
		final PsiClass psiClass = getContainingClassElement();
		if(psiClass == null)
		{
			return null;
		}

		return psiClass.getContainingFile();
	}

	@Override
	public PsiElement setName(@Nonnull String name) throws IncorrectOperationException
	{
		DynamicManager.getInstance(getProject()).replaceDynamicPropertyName(myContainingClassName, getName(), name);
		return super.setName(name);
	}

	@Override
	public String getPresentableText()
	{
		return getName();
	}

	@Override
	@javax.annotation.Nullable
	public String getLocationString()
	{
		return null;
	}

	@Override
	@Nonnull
	public SearchScope getUseScope()
	{
		return GlobalSearchScope.projectScope(myProject);
	}

	@Override
	public void navigate(boolean requestFocus)
	{
		if(canNavigateToSource())
		{
			super.navigate(requestFocus);
			return;
		}
		DynamicToolWindowWrapper.getInstance(myProject).getToolWindow().activate(new Runnable()
		{
			@Override
			public void run()
			{
				DynamicToolWindowWrapper toolWindowWrapper = DynamicToolWindowWrapper.getInstance(myProject);
				final TreeTable treeTable = toolWindowWrapper.getTreeTable();
				final ListTreeTableModelOnColumns model = toolWindowWrapper.getTreeTableModel();

				Object root = model.getRoot();

				if(root == null || !(root instanceof DefaultMutableTreeNode))
				{
					return;
				}

				DefaultMutableTreeNode treeRoot = ((DefaultMutableTreeNode) root);
				final PsiClass psiClass = getContainingClassElement();
				if(psiClass == null)
				{
					return;
				}

				final DefaultMutableTreeNode desiredNode;
				DPropertyElement dynamicProperty = null;
				PsiClass trueSuper = null;
				for(PsiClass aSuper : PsiUtil.iterateSupers(psiClass, true))
				{
					dynamicProperty = DynamicManager.getInstance(myProject).findConcreteDynamicProperty(aSuper
							.getQualifiedName(), getName());

					if(dynamicProperty != null)
					{
						trueSuper = aSuper;
						break;
					}
				}

				if(trueSuper == null)
				{
					return;
				}

				final DefaultMutableTreeNode classNode = TreeUtil.findNodeWithObject(treeRoot,
						new DClassElement(myProject, trueSuper.getQualifiedName()));
				if(classNode == null)
				{
					return;
				}

				desiredNode = TreeUtil.findNodeWithObject(classNode, dynamicProperty);

				if(desiredNode == null)
				{
					return;
				}
				final TreePath path = TreeUtil.getPathFromRoot(desiredNode);

				treeTable.getTree().expandPath(path);
				treeTable.getTree().setSelectionPath(path);
				treeTable.getTree().fireTreeExpanded(path);

				ToolWindowManager.getInstance(myProject).getFocusManager().requestFocus(treeTable, true);
				treeTable.revalidate();
				treeTable.repaint();

			}
		}, true);
	}

	@Override
	public boolean canNavigateToSource()
	{
		return false;
	}

	@Override
	public boolean canNavigate()
	{
		return true;
	}

	@Override
	public boolean isWritable()
	{
		return true;
	}

	@Override
	@Nullable
	public Icon getIcon(boolean open)
	{
		return JetgroovyIcons.Groovy.Property;
	}

	@Override
	public boolean isValid()
	{
		return true;
	}

	@Override
	public PsiClass getContainingClass()
	{
		return getContainingClassElement();
	}

	@Override
	public PsiDocComment getDocComment()
	{
		return null;
	}

	@Override
	public boolean isDeprecated()
	{
		return false;
	}

	@Override
	public void setInitializer(@Nullable PsiExpression initializer) throws IncorrectOperationException
	{
		throw new IncorrectOperationException();
	}

	@Nonnull
	@Override
	public PsiType getType()
	{
		PsiType type = super.getType();
		if(type instanceof PsiClassType && ((PsiClassType) type).resolve() == null)
		{
			return TypesUtil.getJavaLangObject(this);
		}
		return type;
	}
}
