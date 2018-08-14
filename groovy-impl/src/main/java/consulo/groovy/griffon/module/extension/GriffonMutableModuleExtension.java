/*
 * Copyright 2013 Consulo.org
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
package consulo.groovy.griffon.module.extension;

import javax.swing.JComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.groovy.griffon.module.extension.GriffonModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.roots.ModuleRootLayer;

/**
 * @author VISTALL
 * @since 14:33/30.06.13
 */
public class GriffonMutableModuleExtension extends GriffonModuleExtension implements MutableModuleExtension<GriffonModuleExtension>
{
	public GriffonMutableModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer module)
	{
		super(id, module);
	}

	@javax.annotation.Nullable
	@Override
	public JComponent createConfigurablePanel(@Nullable Runnable updateOnCheck)
	{
		return null;
	}

	@Override
	public void setEnabled(boolean val)
	{
		myIsEnabled = val;
	}

	@Override
	public boolean isModified(@Nonnull GriffonModuleExtension extension)
	{
		return myIsEnabled != extension.isEnabled();
	}
}