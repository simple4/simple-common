/**
 * MVEL 2.0
 * Copyright (C) 2007 The Codehaus
 * Mike Brock, Dhanji Prasanna, John Graham, Mark Proctor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simpleframework.lib.org.mvel2.integration.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.simpleframework.lib.org.mvel2.ParserContext;
import net.simpleframework.lib.org.mvel2.integration.VariableResolver;
import net.simpleframework.lib.org.mvel2.integration.VariableResolverFactory;

public class TypeInjectionResolverFactoryImpl extends MapVariableResolverFactory implements
		TypeInjectionResolverFactory {
	public TypeInjectionResolverFactoryImpl() {
		this.variables = new HashMap();
	}

	public TypeInjectionResolverFactoryImpl(final Map<String, Object> variables) {
		this.variables = variables;
	}

	public TypeInjectionResolverFactoryImpl(final ParserContext ctx,
			final VariableResolverFactory nextVariableResolverFactory) {
		super(ctx.getImports(), ctx.hasFunction() ? new TypeInjectionResolverFactoryImpl(
				ctx.getFunctions(), nextVariableResolverFactory) : nextVariableResolverFactory);
	}

	public TypeInjectionResolverFactoryImpl(final Map<String, Object> variables,
			final VariableResolverFactory nextFactory) {
		super(variables, nextFactory);
	}

	public TypeInjectionResolverFactoryImpl(final Map<String, Object> variables,
			final boolean cachingSafe) {
		super(variables);
	}

	@Override
	public VariableResolver createVariable(final String name, final Object value) {
		if (nextFactory == null) {
			nextFactory = new MapVariableResolverFactory(new HashMap());
		}
		/**
		 * Delegate to the next factory.
		 */
		return nextFactory.createVariable(name, value);
	}

	@Override
	public VariableResolver createVariable(final String name, final Object value, final Class<?> type) {
		if (nextFactory == null) {
			nextFactory = new MapVariableResolverFactory(new HashMap());
		}
		/**
		 * Delegate to the next factory.
		 */
		return nextFactory.createVariable(name, value, type);
	}

	@Override
	public Set<String> getKnownVariables() {
		if (nextFactory == null) {
			return new HashSet<String>(0);
		} else {
			return nextFactory.getKnownVariables();
		}
	}
}
