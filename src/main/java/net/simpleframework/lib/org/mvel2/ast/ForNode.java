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

import static net.simpleframework.lib.org.mvel2.util.CompilerTools.expectType;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.subCompileExpression;

import java.util.HashMap;

import net.simpleframework.lib.org.mvel2.CompileException;
import net.simpleframework.lib.org.mvel2.ParserContext;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableStatement;
import net.simpleframework.lib.org.mvel2.integration.VariableResolverFactory;
import net.simpleframework.lib.org.mvel2.integration.impl.MapVariableResolverFactory;
import net.simpleframework.lib.org.mvel2.util.ParseTools;

/**
 * @author Christopher Brock
 */
public class ForNode extends BlockNode {
	protected String item;

	protected ExecutableStatement initializer;
	protected ExecutableStatement condition;

	protected ExecutableStatement after;

	protected boolean indexAlloc = false;

	public ForNode(final char[] expr, final int start, final int offset, final int blockStart,
			final int blockEnd, final int fields, final ParserContext pCtx) {
		super(pCtx);

		final boolean varsEscape = buildForEach(this.expr = expr, this.start = start,
				this.offset = offset, this.blockStart = blockStart, this.blockOffset = blockEnd,
				fields, pCtx);

		this.indexAlloc = pCtx != null && pCtx.isIndexAllocation();

		if ((fields & COMPILE_IMMEDIATE) != 0 && compiledBlock.isEmptyStatement() && !varsEscape) {
			throw new RedundantCodeException();
		}

		if (pCtx != null) {
			pCtx.popVariableScope();
		}
	}

	@Override
	public Object getReducedValueAccelerated(final Object ctx, final Object thisValue,
			final VariableResolverFactory factory) {
		final VariableResolverFactory ctxFactory = indexAlloc ? factory
				: new MapVariableResolverFactory(new HashMap<String, Object>(1), factory);
		Object v;
		for (initializer.getValue(ctx, thisValue, ctxFactory); (Boolean) condition.getValue(ctx,
				thisValue, ctxFactory); after.getValue(ctx, thisValue, ctxFactory)) {
			v = compiledBlock.getValue(ctx, thisValue, ctxFactory);
			if (ctxFactory.tiltFlag()) {
				return v;
			}
		}
		return null;
	}

	@Override
	public Object getReducedValue(final Object ctx, final Object thisValue,
			VariableResolverFactory factory) {
		Object v;
		for (initializer.getValue(ctx, thisValue, factory = new MapVariableResolverFactory(
				new HashMap<String, Object>(1), factory)); (Boolean) condition.getValue(ctx, thisValue,
				factory); after.getValue(ctx, thisValue, factory)) {
			v = compiledBlock.getValue(ctx, thisValue, factory);
			if (factory.tiltFlag()) {
				return v;
			}
		}

		return null;
	}

	private boolean buildForEach(final char[] condition, int start, final int offset,
			final int blockStart, final int blockEnd, final int fields, final ParserContext pCtx) {
		final int end = start + offset;
		int cursor = nextCondPart(condition, start, end, false);

		boolean varsEscape = false;

		try {
			ParserContext spCtx = pCtx;
			if (pCtx != null) {
				spCtx = pCtx.createSubcontext().createColoringSubcontext();
			} else {
				spCtx = new ParserContext();
			}

			this.initializer = (ExecutableStatement) subCompileExpression(condition, start, cursor
					- start - 1, spCtx);

			if (pCtx != null) {
				pCtx.pushVariableScope();
			}

			try {
				expectType(
						this.condition = (ExecutableStatement) subCompileExpression(condition,
								start = cursor, (cursor = nextCondPart(condition, start, end, false))
										- start - 1, spCtx), Boolean.class,
						((fields & COMPILE_IMMEDIATE) != 0));
			} catch (final CompileException e) {
				if (e.getExpr().length == 0) {
					e.setExpr(expr);

					while (start < expr.length && ParseTools.isWhitespace(expr[start])) {
						start++;
					}

					e.setCursor(start);
				}
				throw e;
			}

			this.after = (ExecutableStatement) subCompileExpression(condition, start = cursor,
					(nextCondPart(condition, start, end, true)) - start, spCtx);

			if (spCtx != null && (fields & COMPILE_IMMEDIATE) != 0 && spCtx.isVariablesEscape()) {
				if (pCtx != spCtx) {
					pCtx.addVariables(spCtx.getVariables());
				}
				varsEscape = true;
			} else if (spCtx != null && pCtx != null) {
				pCtx.addVariables(spCtx.getVariables());
			}

			this.compiledBlock = (ExecutableStatement) subCompileExpression(expr, blockStart,
					blockEnd, spCtx);

		} catch (final NegativeArraySizeException e) {
			throw new CompileException("wrong syntax; did you mean to use 'foreach'?", expr, start);
		}
		return varsEscape;
	}

	private static int nextCondPart(final char[] condition, int cursor, final int end,
			final boolean allowEnd) {
		for (; cursor < end; cursor++) {
			if (condition[cursor] == ';') {
				return ++cursor;
			}
		}
		if (!allowEnd) {
			throw new CompileException("expected ;", condition, cursor);
		}
		return cursor;
	}
}