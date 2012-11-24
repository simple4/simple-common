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

import static java.lang.String.valueOf;
import static java.util.regex.Pattern.compile;
import static net.simpleframework.lib.org.mvel2.MVEL.eval;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.subCompileExpression;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import net.simpleframework.lib.org.mvel2.CompileException;
import net.simpleframework.lib.org.mvel2.ParserContext;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableLiteral;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableStatement;
import net.simpleframework.lib.org.mvel2.integration.VariableResolverFactory;

public class RegExMatch extends ASTNode {
	private ExecutableStatement stmt;
	private ExecutableStatement patternStmt;

	private final int patternStart;
	private final int patternOffset;
	private Pattern p;

	public RegExMatch(final char[] expr, final int start, final int offset, final int fields,
			final int patternStart, final int patternOffset, final ParserContext pCtx) {
		super(pCtx);
		this.expr = expr;
		this.start = start;
		this.offset = offset;
		this.patternStart = patternStart;
		this.patternOffset = patternOffset;

		if ((fields & COMPILE_IMMEDIATE) != 0) {
			this.stmt = (ExecutableStatement) subCompileExpression(expr, start, offset);
			if ((this.patternStmt = (ExecutableStatement) subCompileExpression(expr, patternStart,
					patternOffset, pCtx)) instanceof ExecutableLiteral) {

				try {
					p = compile(valueOf(patternStmt.getValue(null, null)));
				} catch (final PatternSyntaxException e) {
					throw new CompileException("bad regular expression", expr, patternStart, e);
				}
			}
		}
	}

	@Override
	public Object getReducedValueAccelerated(final Object ctx, final Object thisValue,
			final VariableResolverFactory factory) {
		if (p == null) {
			return compile(valueOf(patternStmt.getValue(ctx, thisValue, factory))).matcher(
					valueOf(stmt.getValue(ctx, thisValue, factory))).matches();
		} else {
			return p.matcher(valueOf(stmt.getValue(ctx, thisValue, factory))).matches();
		}
	}

	@Override
	public Object getReducedValue(final Object ctx, final Object thisValue,
			final VariableResolverFactory factory) {
		try {
			return compile(valueOf(eval(expr, patternStart, patternOffset, ctx, factory))).matcher(
					valueOf(eval(expr, start, offset, ctx, factory))).matches();
		} catch (final PatternSyntaxException e) {
			throw new CompileException("bad regular expression", expr, patternStart, e);
		}
	}

	@Override
	public Class getEgressType() {
		return Boolean.class;
	}

	public Pattern getPattern() {
		return p;
	}

	public ExecutableStatement getStatement() {
		return stmt;
	}
}
