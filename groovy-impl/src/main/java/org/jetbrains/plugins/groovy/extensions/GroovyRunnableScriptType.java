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
package org.jetbrains.plugins.groovy.extensions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import consulo.execution.action.Location;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.runner.GroovyScriptRunConfiguration;
import org.jetbrains.plugins.groovy.runner.GroovyScriptRunner;
import consulo.execution.action.Location;

public abstract class GroovyRunnableScriptType extends GroovyScriptType
{
	public GroovyRunnableScriptType(String id)
	{
		super(id);
	}

	public void tuneConfiguration(@Nonnull GroovyFile file,
			@Nonnull GroovyScriptRunConfiguration configuration,
			Location location)
	{
	}

	@Nullable
	public GroovyScriptRunner getRunner()
	{
		return null;
	}

	public boolean isConfigurationByLocation(@Nonnull GroovyScriptRunConfiguration existing,
			@Nonnull Location location)
	{
		return true;
	}
}
