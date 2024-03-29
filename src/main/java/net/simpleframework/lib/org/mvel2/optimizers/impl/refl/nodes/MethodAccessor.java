/**
 * MVEL (The MVFLEX Expression Language)
 *
 * Copyright (C) 2007 Christopher Brock, MVFLEX/Valhalla Project and the Codehaus
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
 *
 */
package net.simpleframework.lib.org.mvel2.optimizers.impl.refl.nodes;

import static net.simpleframework.lib.org.mvel2.DataConversion.convert;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.getBestCandidate;
import static net.simpleframework.lib.org.mvel2.util.ParseTools.getWidenedTarget;

import java.lang.reflect.Array;
import java.lang.reflect.Method;

import net.simpleframework.lib.org.mvel2.compiler.AccessorNode;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableStatement;
import net.simpleframework.lib.org.mvel2.integration.VariableResolverFactory;

public class MethodAccessor implements AccessorNode {
	private AccessorNode nextNode;

	private Method method;
	private Class[] parameterTypes;
	private ExecutableStatement[] parms;
	private int length;
	private boolean coercionNeeded = false;

	@Override
	public Object getValue(final Object ctx, final Object elCtx, final VariableResolverFactory vars) {
		if (!coercionNeeded) {
			try {
				if (nextNode != null) {
					return nextNode.getValue(method.invoke(ctx, executeAll(elCtx, vars, method)), elCtx,
							vars);
				} else {
					return method.invoke(ctx, executeAll(elCtx, vars, method));
				}
			} catch (final IllegalArgumentException e) {
				if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
					final Method o = getBestCandidate(parameterTypes, method.getName(), ctx.getClass(),
							ctx.getClass().getMethods(), true);
					if (o != null) {
						return executeOverrideTarget(getWidenedTarget(o), ctx, elCtx, vars);
					}
				}

				coercionNeeded = true;
				return getValue(ctx, elCtx, vars);
			} catch (final Exception e) {
				throw new RuntimeException("cannot invoke method: " + method.getName(), e);
			}

		} else {
			try {
				if (nextNode != null) {
					return nextNode.getValue(
							method.invoke(ctx,
									executeAndCoerce(parameterTypes, elCtx, vars, method.isVarArgs())),
							elCtx, vars);
				} else {
					return method.invoke(ctx,
							executeAndCoerce(parameterTypes, elCtx, vars, method.isVarArgs()));
				}
			} catch (final IllegalArgumentException e) {
				final Object[] vs = executeAndCoerce(parameterTypes, elCtx, vars, false);
				Method newMeth;
				if ((newMeth = getWidenedTarget(getBestCandidate(vs, method.getName(), ctx.getClass(),
						ctx.getClass().getMethods(), false))) != null) {
					return executeOverrideTarget(newMeth, ctx, elCtx, vars);
				} else {
					throw e;
				}
			} catch (final Exception e) {
				throw new RuntimeException("cannot invoke method: " + method.getName(), e);
			}
		}
	}

	private Object executeOverrideTarget(final Method o, final Object ctx, final Object elCtx,
			final VariableResolverFactory vars) {
		if (!coercionNeeded) {
			try {
				try {
					if (nextNode != null) {
						return nextNode.getValue(o.invoke(ctx, executeAll(elCtx, vars, o)), elCtx, vars);
					} else {
						return o.invoke(ctx, executeAll(elCtx, vars, o));
					}
				} catch (final IllegalArgumentException e) {
					if (coercionNeeded) {
						throw e;
					}

					coercionNeeded = true;
					return executeOverrideTarget(o, ctx, elCtx, vars);
				}
			} catch (final Exception e2) {
				throw new RuntimeException("unable to invoke method", e2);
			}
		} else {
			try {
				if (nextNode != null) {
					return nextNode.getValue(
							o.invoke(ctx,
									executeAndCoerce(o.getParameterTypes(), elCtx, vars, o.isVarArgs())),
							elCtx, vars);
				} else {
					return o.invoke(ctx,
							executeAndCoerce(o.getParameterTypes(), elCtx, vars, o.isVarArgs()));
				}
			} catch (final IllegalAccessException e) {
				throw new RuntimeException("unable to invoke method (expected target: "
						+ method.getDeclaringClass().getName() + "::" + method.getName() + "; "
						+ "actual target: " + ctx.getClass().getName() + "::" + method.getName()
						+ "; coercionNeeded=" + (coercionNeeded ? "yes" : "no") + ")");
			} catch (final Exception e2) {
				throw new RuntimeException("unable to invoke method (expected target: "
						+ method.getDeclaringClass().getName() + "::" + method.getName() + "; "
						+ "actual target: " + ctx.getClass().getName() + "::" + method.getName()
						+ "; coercionNeeded=" + (coercionNeeded ? "yes" : "no") + ")");
			}
		}
	}

	private Object[] executeAll(final Object ctx, final VariableResolverFactory vars, final Method m) {
		if (length == 0) {
			return GetterAccessor.EMPTY;
		}

		final Object[] vals = new Object[length];
		for (int i = 0; i < length - (m.isVarArgs() ? 1 : 0); i++) {
			vals[i] = parms[i].getValue(ctx, vars);
		}

		if (m.isVarArgs()) {
			if (parms.length == length) {
				final Object lastParam = parms[length - 1].getValue(ctx, vars);
				vals[length - 1] = lastParam.getClass().isArray() ? lastParam
						: new Object[] { lastParam };
			} else {
				final Object[] vararg = new Object[parms.length - length + 1];
				for (int i = 0; i < vararg.length; i++) {
					vararg[i] = parms[parms.length - length + i].getValue(ctx, vars);
				}
				vals[length - 1] = vararg;
			}
		}

		return vals;
	}

	private Object[] executeAndCoerce(final Class[] target, final Object elCtx,
			final VariableResolverFactory vars, final boolean isVarargs) {
		final Object[] values = new Object[length];
		for (int i = 0; i < length && !(isVarargs && i >= length - 1); i++) {
			// noinspection unchecked
			values[i] = convert(parms[i].getValue(elCtx, vars), target[i]);
		}
		if (isVarargs) {
			final Class<?> componentType = target[length - 1].getComponentType();
			final Object vararg = Array.newInstance(componentType, parms.length - length + 1);
			for (int i = length - 1; i < parms.length; i++) {
				Array.set(vararg, i - length + 1,
						convert(parms[i].getValue(elCtx, vars), componentType));
			}
			values[length - 1] = vararg;
		}
		return values;
	}

	public Method getMethod() {
		return method;
	}

	public void setMethod(final Method method) {
		this.method = method;
		this.length = (this.parameterTypes = this.method.getParameterTypes()).length;
	}

	public ExecutableStatement[] getParms() {
		return parms;
	}

	public void setParms(final ExecutableStatement[] parms) {
		this.parms = parms;
	}

	public MethodAccessor() {
	}

	public MethodAccessor(final Method method, final ExecutableStatement[] parms) {
		this.method = method;
		this.length = (this.parameterTypes = this.method.getParameterTypes()).length;

		this.parms = parms;
	}

	@Override
	public AccessorNode getNextNode() {
		return nextNode;
	}

	@Override
	public AccessorNode setNextNode(final AccessorNode nextNode) {
		return this.nextNode = nextNode;
	}

	@Override
	public Object setValue(final Object ctx, final Object elCtx,
			final VariableResolverFactory variableFactory, final Object value) {
		try {
			return nextNode.setValue(method.invoke(ctx, executeAll(elCtx, variableFactory, method)),
					elCtx, variableFactory, value);
		} catch (final IllegalArgumentException e) {
			if (ctx != null && method.getDeclaringClass() != ctx.getClass()) {
				final Method o = getBestCandidate(parameterTypes, method.getName(), ctx.getClass(), ctx
						.getClass().getMethods(), true);
				if (o != null) {
					return nextNode.setValue(executeOverrideTarget(o, ctx, elCtx, variableFactory),
							elCtx, variableFactory, value);
				}
			}

			coercionNeeded = true;
			return setValue(ctx, elCtx, variableFactory, value);
		} catch (final Exception e) {
			throw new RuntimeException("cannot invoke method", e);
		}
	}

	@Override
	public Class getKnownEgressType() {
		return method.getReturnType();
	}

	public Class[] getParameterTypes() {
		return parameterTypes;
	}
}
