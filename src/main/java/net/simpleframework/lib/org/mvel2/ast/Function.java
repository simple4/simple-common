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

package net.simpleframework.lib.org.mvel2.ast;

import static net.simpleframework.lib.org.mvel2.util.ParseTools.parseParameterDefList;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.subCompileExpression;

import java.util.Map;

import net.simpleframework.lib.org.mvel2.CompileException;
import net.simpleframework.lib.org.mvel2.ParserContext;
import net.simpleframework.lib.org.mvel2.compiler.AbstractParser;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableStatement;
import net.simpleframework.lib.org.mvel2.compiler.ExpressionCompiler;
import net.simpleframework.lib.org.mvel2.integration.VariableResolver;
import net.simpleframework.lib.org.mvel2.integration.VariableResolverFactory;
import net.simpleframework.lib.org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import net.simpleframework.lib.org.mvel2.integration.impl.FunctionVariableResolverFactory;
import net.simpleframework.lib.org.mvel2.integration.impl.StackDemarcResolverFactory;

@SuppressWarnings({ "unchecked" })
public class Function extends ASTNode implements Safe {
	protected String name;
	protected ExecutableStatement compiledBlock;

	protected String[] parameters;
	protected int parmNum;
	protected boolean cMode = false;

	public Function(final String name, final char[] expr, final int start, final int offset,
			final int blockStart, final int blockOffset, final int fields, final ParserContext pCtx) {
		super(pCtx);
		if ((this.name = name) == null || name.length() == 0) {
			this.name = null;
		}
		this.expr = expr;

		parmNum = (this.parameters = parseParameterDefList(expr, start, offset)).length;

		pCtx.declareFunction(this);

		final ParserContext ctx = new ParserContext(pCtx.getParserConfiguration());

		/**
		 * To prevent the function parameters from being counted as external
		 * inputs, we must add them explicitly here.
		 */
		for (final String s : this.parameters) {
			ctx.addVariable(s, Object.class);
			ctx.addIndexedInput(s);
		}

		/**
		 * Compile the expression so we can determine the input-output delta.
		 */

		ctx.setIndexAllocation(false);
		final ExpressionCompiler compiler = new ExpressionCompiler(expr, blockStart, blockOffset);
		compiler.setVerifyOnly(true);
		compiler.compile(ctx);

		ctx.setIndexAllocation(true);

		/**
		 * Add globals as inputs
		 */
		if (pCtx.getVariables() != null) {
			for (final Map.Entry<String, Class> e : pCtx.getVariables().entrySet()) {
				ctx.getVariables().remove(e.getKey());
				ctx.addInput(e.getKey(), e.getValue());
			}

			ctx.processTables();
		}

		ctx.addIndexedInputs(ctx.getVariables().keySet());
		ctx.getVariables().clear();

		this.compiledBlock = (ExecutableStatement) subCompileExpression(expr, blockStart,
				blockOffset, ctx);

		AbstractParser.setCurrentThreadParserContext(pCtx);

		this.parameters = new String[ctx.getIndexedInputs().size()];

		int i = 0;
		for (final String s : ctx.getIndexedInputs()) {
			this.parameters[i++] = s;
		}

		cMode = (fields & COMPILE_IMMEDIATE) != 0;

		this.egressType = this.compiledBlock.getKnownEgressType();

		pCtx.addVariable(name, Function.class);
	}

	@Override
	public Object getReducedValueAccelerated(final Object ctx, final Object thisValue,
			final VariableResolverFactory factory) {
		if (name != null) {
			if (!factory.isIndexedFactory() && factory.isResolveable(name)) {
				throw new CompileException("duplicate function: " + name, expr, start);
			}
			factory.createVariable(name, this);
		}
		return this;
	}

	@Override
	public Object getReducedValue(final Object ctx, final Object thisValue,
			final VariableResolverFactory factory) {
		if (name != null) {
			if (!factory.isIndexedFactory() && factory.isResolveable(name)) {
				throw new CompileException("duplicate function: " + name, expr, start);
			}
			factory.createVariable(name, this);
		}
		return this;
	}

	public Object call(final Object ctx, final Object thisValue,
			final VariableResolverFactory factory, final Object[] parms) {
		if (parms != null && parms.length != 0) {
			// detect tail recursion
			if (factory instanceof FunctionVariableResolverFactory
					&& ((FunctionVariableResolverFactory) factory).getIndexedVariableResolvers().length == parms.length) {
				final FunctionVariableResolverFactory fvrf = (FunctionVariableResolverFactory) factory;
				if (fvrf.getFunction().equals(this)) {
					final VariableResolver[] swapVR = fvrf.getIndexedVariableResolvers();
					fvrf.updateParameters(parms);
					try {
						return compiledBlock.getValue(ctx, thisValue, fvrf);
					} finally {
						fvrf.setIndexedVariableResolvers(swapVR);
					}
				}
			}
			return compiledBlock.getValue(thisValue, new StackDemarcResolverFactory(
					new FunctionVariableResolverFactory(this, factory, parameters, parms)));
		} else if (cMode) {
			return compiledBlock.getValue(thisValue, new StackDemarcResolverFactory(
					new DefaultLocalVariableResolverFactory(factory, parameters)));
		} else {
			return compiledBlock.getValue(thisValue, new StackDemarcResolverFactory(
					new DefaultLocalVariableResolverFactory(factory, parameters)));
		}

	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String[] getParameters() {
		return parameters;
	}

	public void setParameters(final String[] parameters) {
		this.parameters = parameters;
	}

	public boolean hasParameters() {
		return this.parameters != null && this.parameters.length != 0;
	}

	public void checkArgumentCount(final int passing) {
		if (passing != parmNum) {
			throw new CompileException("bad number of arguments in function call: " + passing
					+ " (expected: " + (parmNum == 0 ? "none" : parmNum) + ")", expr, start);
		}
	}

	public ExecutableStatement getCompiledBlock() {
		return compiledBlock;
	}

	@Override
	public String toString() {
		return "FunctionDef:" + (name == null ? "Anonymous" : name);
	}
}
