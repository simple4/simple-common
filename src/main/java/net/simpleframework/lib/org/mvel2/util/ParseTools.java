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

package net.simpleframework.lib.org.mvel2.util;

import static java.lang.Class.forName;
import static java.lang.Double.parseDouble;
import static java.lang.String.valueOf;
import static java.lang.System.arraycopy;
import static java.lang.Thread.currentThread;
import static java.nio.ByteBuffer.allocateDirect;
import static net.simpleframework.lib.org.mvel2.DataConversion.canConvert;
import static net.simpleframework.lib.org.mvel2.DataTypes.DOUBLE;
import static net.simpleframework.lib.org.mvel2.DataTypes.INTEGER;
import static net.simpleframework.lib.org.mvel2.DataTypes.LONG;
import static net.simpleframework.lib.org.mvel2.MVEL.getDebuggingOutputFileName;
import static net.simpleframework.lib.org.mvel2.compiler.AbstractParser.LITERALS;
import static net.simpleframework.lib.org.mvel2.integration.ResolverTools.appendFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.simpleframework.lib.org.mvel2.CompileException;
import net.simpleframework.lib.org.mvel2.DataTypes;
import net.simpleframework.lib.org.mvel2.MVEL;
import net.simpleframework.lib.org.mvel2.Operator;
import net.simpleframework.lib.org.mvel2.OptimizationFailure;
import net.simpleframework.lib.org.mvel2.ParserContext;
import net.simpleframework.lib.org.mvel2.ast.ASTNode;
import net.simpleframework.lib.org.mvel2.compiler.AbstractParser;
import net.simpleframework.lib.org.mvel2.compiler.BlankLiteral;
import net.simpleframework.lib.org.mvel2.compiler.CompiledExpression;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableAccessor;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableAccessorSafe;
import net.simpleframework.lib.org.mvel2.compiler.ExecutableLiteral;
import net.simpleframework.lib.org.mvel2.compiler.ExpressionCompiler;
import net.simpleframework.lib.org.mvel2.integration.VariableResolverFactory;
import net.simpleframework.lib.org.mvel2.integration.impl.ClassImportResolverFactory;
import net.simpleframework.lib.org.mvel2.math.MathProcessor;

@SuppressWarnings({ "ManualArrayCopy" })
public class ParseTools {
	public static final String[] EMPTY_STR_ARR = new String[0];
	public static final Object[] EMPTY_OBJ_ARR = new Object[0];
	public static final Class[] EMPTY_CLS_ARR = new Class[0];

	static {
		try {
			final double version = parseDouble(System.getProperty("java.version").substring(0, 3));
			if (version < 1.5) {
				throw new RuntimeException("unsupported java version: " + version);
			}
		} catch (final RuntimeException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException("unable to initialize math processor", e);
		}
	}

	public static List<char[]> parseMethodOrConstructor(final char[] parm) {
		int start = -1;
		for (int i = 0; i < parm.length; i++) {
			if (parm[i] == '(') {
				start = ++i;
				break;
			}
		}
		if (start != -1) {
			return parseParameterList(parm, --start + 1, balancedCapture(parm, start, '(') - start - 1);
		}

		return Collections.emptyList();
	}

	public static String[] parseParameterDefList(final char[] parm, final int offset, int length) {
		final List<String> list = new LinkedList<String>();

		if (length == -1) {
			length = parm.length;
		}

		int start = offset;
		int i = offset;
		final int end = i + length;
		String s;

		for (; i < end; i++) {
			switch (parm[i]) {
			case '(':
			case '[':
			case '{':
				i = balancedCapture(parm, i, parm[i]);
				continue;

			case '\'':
				i = captureStringLiteral('\'', parm, i, parm.length);
				continue;

			case '"':
				i = captureStringLiteral('"', parm, i, parm.length);
				continue;

			case ',':
				if (i > start) {
					while (isWhitespace(parm[start])) {
						start++;
					}

					checkNameSafety(s = new String(parm, start, i - start));

					list.add(s);
				}

				while (isWhitespace(parm[i])) {
					i++;
				}

				start = i + 1;
				continue;

			default:
				if (!isWhitespace(parm[i]) && !isIdentifierPart(parm[i])) {
					throw new CompileException("expected parameter", parm, start);
				}
			}
		}

		if (start < (length + offset) && i > start) {
			if ((s = createStringTrimmed(parm, start, i - start)).length() > 0) {
				checkNameSafety(s);
				list.add(s);
			}
		} else if (list.size() == 0) {
			if ((s = createStringTrimmed(parm, start, length)).length() > 0) {
				checkNameSafety(s);
				list.add(s);
			}
		}

		return list.toArray(new String[list.size()]);
	}

	public static List<char[]> parseParameterList(final char[] parm, final int offset, int length) {
		final List<char[]> list = new ArrayList<char[]>();

		if (length == -1) {
			length = parm.length;
		}

		int start = offset;
		int i = offset;
		final int end = i + length;

		for (; i < end; i++) {
			switch (parm[i]) {
			case '(':
			case '[':
			case '{':
				i = balancedCapture(parm, i, parm[i]);
				continue;

			case '\'':
				i = captureStringLiteral('\'', parm, i, parm.length);
				continue;

			case '"':
				i = captureStringLiteral('"', parm, i, parm.length);
				continue;

			case ',':
				if (i > start) {
					while (isWhitespace(parm[start])) {
						start++;
					}

					list.add(subsetTrimmed(parm, start, i - start));
				}

				while (isWhitespace(parm[i])) {
					i++;
				}

				start = i + 1;
			}
		}

		if (start < (length + offset) && i > start) {
			final char[] s = subsetTrimmed(parm, start, i - start);
			if (s.length > 0) {
				list.add(s);
			}
		} else if (list.size() == 0) {
			final char[] s = subsetTrimmed(parm, start, length);
			if (s.length > 0) {
				list.add(s);
			}
		}

		return list;
	}

	private static Map<String, Map<Integer, WeakReference<Method>>> RESOLVED_METH_CACHE = new WeakHashMap<String, Map<Integer, WeakReference<Method>>>(
			10);

	public static Method getBestCandidate(final Object[] arguments, final String method,
			final Class decl, final Method[] methods, final boolean requireExact) {
		final Class[] targetParms = new Class[arguments.length];
		for (int i = 0; i != arguments.length; i++) {
			targetParms[i] = arguments[i] != null ? arguments[i].getClass() : null;
		}
		return getBestCandidate(targetParms, method, decl, methods, requireExact);
	}

	public static Method getBestCandidate(final Class[] arguments, final String method,
			final Class decl, final Method[] methods, final boolean requireExact) {
		return getBestCandidate(arguments, method, decl, methods, requireExact, false);
	}

	public static Method getBestCandidate(final Class[] arguments, final String method,
			final Class decl, Method[] methods, final boolean requireExact, final boolean classTarget) {

		if (methods.length == 0) {
			return null;
		}

		Class[] parmTypes;
		Method bestCandidate = null;
		int bestScore = 0;
		boolean retry = false;

		final Integer hash = createClassSignatureHash(decl, arguments);

		Map<Integer, WeakReference<Method>> methCache = RESOLVED_METH_CACHE.get(method);

		WeakReference<Method> ref;

		if (methCache != null && (ref = methCache.get(hash)) != null
				&& (bestCandidate = ref.get()) != null) {
			return bestCandidate;
		}

		do {
			for (final Method meth : methods) {
				if (classTarget && (meth.getModifiers() & Modifier.STATIC) == 0) {
					continue;
				}

				if (method.equals(meth.getName())) {
					final boolean isVarArgs = meth.isVarArgs();
					if ((parmTypes = meth.getParameterTypes()).length != arguments.length && !isVarArgs) {
						continue;
					} else if (arguments.length == 0 && parmTypes.length == 0) {
						if (bestCandidate == null
								|| bestCandidate.getReturnType().isAssignableFrom(meth.getReturnType())) {
							bestCandidate = meth;
						}
						continue;
					}

					final int score = getMethodScore(arguments, requireExact, parmTypes, isVarArgs);
					if (score != 0) {
						if (score > bestScore) {
							bestCandidate = meth;
							bestScore = score;
						} else if (score == bestScore) {
							if (bestCandidate == null
									|| (bestCandidate.getReturnType() != meth.getReturnType() && bestCandidate
											.getReturnType().isAssignableFrom(meth.getReturnType()))) {
								bestCandidate = meth;
							}
						}
					}
				}
			}

			if (bestCandidate != null) {
				if (methCache == null) {
					RESOLVED_METH_CACHE.put(method,
							methCache = new WeakHashMap<Integer, WeakReference<Method>>());
				}

				methCache.put(hash, new WeakReference<Method>(bestCandidate));

				break;
			}

			if (!retry && decl.isInterface()) {
				final Method[] objMethods = Object.class.getMethods();
				final Method[] nMethods = new Method[methods.length + objMethods.length];
				for (int i = 0; i < methods.length; i++) {
					nMethods[i] = methods[i];
				}

				for (int i = 0; i < objMethods.length; i++) {
					nMethods[i + methods.length] = objMethods[i];
				}
				methods = nMethods;

				retry = true;
			} else {
				break;
			}
		} while (true);

		return bestCandidate;
	}

	private static int getMethodScore(final Class[] arguments, final boolean requireExact,
			final Class[] parmTypes, final boolean varArgs) {
		int score = 0;
		for (int i = 0; i != arguments.length; i++) {
			Class actualParamType;
			if (varArgs && i >= parmTypes.length - 1) {
				actualParamType = parmTypes[parmTypes.length - 1].getComponentType();
			} else {
				actualParamType = parmTypes[i];
			}

			if (arguments[i] == null) {
				if (!actualParamType.isPrimitive()) {
					score += 6;
				} else {
					score = 0;
					break;
				}
			} else if (actualParamType == arguments[i]) {
				score += 7;
			} else if (actualParamType.isPrimitive() && boxPrimitive(actualParamType) == arguments[i]) {
				score += 6;
			} else if (arguments[i].isPrimitive() && unboxPrimitive(arguments[i]) == actualParamType) {
				score += 6;
			} else if (actualParamType.isAssignableFrom(arguments[i])) {
				score += 5;
			} else if (isNumericallyCoercible(arguments[i], actualParamType)) {
				score += 4;
			} else if (boxPrimitive(actualParamType).isAssignableFrom(boxPrimitive(arguments[i]))
					&& Object.class != arguments[i]) {
				score += 3 + scoreInterface(actualParamType, arguments[i]);
			} else if (!requireExact && canConvert(actualParamType, arguments[i])) {
				if (actualParamType.isArray() && arguments[i].isArray()) {
					score += 1;
				} else if (actualParamType == char.class && arguments[i] == String.class) {
					score += 1;
				}

				score += 1;
			} else if (actualParamType == Object.class || arguments[i] == NullType.class) {
				score += 1;
			} else {
				score = 0;
				break;
			}
		}
		return score;
	}

	public static int scoreInterface(final Class parm, final Class arg) {
		if (parm.isInterface()) {
			final Class[] iface = arg.getInterfaces();
			if (iface != null) {
				for (final Class c : iface) {
					if (c == parm) {
						return 1;
					} else if (parm.isAssignableFrom(c)) {
						return scoreInterface(parm, arg.getSuperclass());
					}
				}
			}
		}
		return 0;
	}

	public static Method getExactMatch(final String name, final Class[] args,
			final Class returnType, final Class cls) {
		for (final Method meth : cls.getMethods()) {
			if (name.equals(meth.getName()) && returnType == meth.getReturnType()) {
				final Class[] parameterTypes = meth.getParameterTypes();
				if (parameterTypes.length != args.length) {
					continue;
				}

				for (int i = 0; i < parameterTypes.length; i++) {
					if (parameterTypes[i] != args[i]) {
						return null;
					}
				}
				return meth;
			}
		}
		return null;
	}

	public static Method getWidenedTarget(final Method method) {
		return getWidenedTarget(method.getDeclaringClass(), method);
	}

	public static Method getWidenedTarget(final Class cls, final Method method) {
		Method m = method, best = method;
		final Class[] args = method.getParameterTypes();
		final String name = method.getName();
		final Class rt = m.getReturnType();

		Class currentCls = cls;
		while (currentCls != null) {
			for (final Class iface : currentCls.getInterfaces()) {
				if ((m = getExactMatch(name, args, rt, iface)) != null) {
					best = m;
				}
			}
			currentCls = currentCls.getSuperclass();
		}

		if (best != method) {
			return best;
		}

		currentCls = cls;
		for (currentCls = cls; currentCls != null; currentCls = currentCls.getSuperclass()) {
			if ((m = getExactMatch(name, args, rt, currentCls)) != null) {
				best = m;
			}
		}
		return best;
	}

	private static Map<Class, Map<Integer, WeakReference<Constructor>>> RESOLVED_CONST_CACHE = new WeakHashMap<Class, Map<Integer, WeakReference<Constructor>>>(
			10);
	private static Map<Constructor, WeakReference<Class[]>> CONSTRUCTOR_PARMS_CACHE = new WeakHashMap<Constructor, WeakReference<Class[]>>(
			10);

	private static Class[] getConstructors(final Constructor cns) {
		final WeakReference<Class[]> ref = CONSTRUCTOR_PARMS_CACHE.get(cns);
		Class[] parms;
		if (ref != null && (parms = ref.get()) != null) {
			return parms;
		} else {
			CONSTRUCTOR_PARMS_CACHE.put(cns,
					new WeakReference<Class[]>(parms = cns.getParameterTypes()));
			return parms;
		}
	}

	public static Constructor getBestConstructorCandidate(final Object[] args, final Class cls,
			final boolean requireExact) {
		final Class[] arguments = new Class[args.length];

		for (int i = 0; i != args.length; i++) {
			if (args[i] != null) {
				arguments[i] = args[i].getClass();
			}
		}

		return getBestConstructorCandidate(arguments, cls, requireExact);
	}

	public static Constructor getBestConstructorCandidate(final Class[] arguments, final Class cls,
			final boolean requireExact) {
		Class[] parmTypes;
		Constructor bestCandidate = null;
		int bestScore = 0;

		final Integer hash = createClassSignatureHash(cls, arguments);

		Map<Integer, WeakReference<Constructor>> cache = RESOLVED_CONST_CACHE.get(cls);
		WeakReference<Constructor> ref;
		if (cache != null && (ref = cache.get(hash)) != null && (bestCandidate = ref.get()) != null) {
			return bestCandidate;
		}

		for (final Constructor construct : getConstructors(cls)) {
			final boolean isVarArgs = construct.isVarArgs();
			if ((parmTypes = getConstructors(construct)).length != arguments.length
					&& !construct.isVarArgs()) {
				continue;
			} else if (arguments.length == 0 && parmTypes.length == 0) {
				return construct;
			}

			final int score = getMethodScore(arguments, requireExact, parmTypes, isVarArgs);
			if (score != 0 && score > bestScore) {
				bestCandidate = construct;
				bestScore = score;
			}
		}

		if (bestCandidate != null) {
			if (cache == null) {
				RESOLVED_CONST_CACHE.put(cls,
						cache = new WeakHashMap<Integer, WeakReference<Constructor>>());
			}
			cache.put(hash, new WeakReference<Constructor>(bestCandidate));
		}

		return bestCandidate;
	}

	private static Map<ClassLoader, Map<String, WeakReference<Class>>> CLASS_RESOLVER_CACHE = new WeakHashMap<ClassLoader, Map<String, WeakReference<Class>>>(
			1, 1.0f);
	private static Map<Class, WeakReference<Constructor[]>> CLASS_CONSTRUCTOR_CACHE = new WeakHashMap<Class, WeakReference<Constructor[]>>(
			10);

	public static Class createClass(final String className, final ParserContext pCtx)
			throws ClassNotFoundException {
		final ClassLoader classLoader = pCtx != null ? pCtx.getClassLoader() : currentThread()
				.getContextClassLoader();

		Map<String, WeakReference<Class>> cache = CLASS_RESOLVER_CACHE.get(classLoader);

		if (cache == null) {
			CLASS_RESOLVER_CACHE.put(classLoader,
					cache = new WeakHashMap<String, WeakReference<Class>>(10));
		}

		WeakReference<Class> ref;
		Class cls = null;

		if ((ref = cache.get(className)) != null && (cls = ref.get()) != null) {
			return cls;
		} else {
			try {
				cls = Class.forName(className, true, classLoader);
			} catch (final ClassNotFoundException e) {
				/**
				 * Now try the system classloader.
				 */
				if (classLoader != Thread.currentThread().getContextClassLoader()) {
					cls = forName(className, true, Thread.currentThread().getContextClassLoader());
				} else {
					throw e;
				}
			}

			cache.put(className, new WeakReference<Class>(cls));
			return cls;
		}
	}

	public static Constructor[] getConstructors(final Class cls) {
		final WeakReference<Constructor[]> ref = CLASS_CONSTRUCTOR_CACHE.get(cls);
		Constructor[] cns;

		if (ref != null && (cns = ref.get()) != null) {
			return cns;
		} else {
			CLASS_CONSTRUCTOR_CACHE.put(cls,
					new WeakReference<Constructor[]>(cns = cls.getConstructors()));
			return cns;
		}
	}

	public static String[] captureContructorAndResidual(final char[] cs, final int start,
			final int offset) {
		int depth = 0;
		final int end = start + offset;
		for (int i = start; i < end; i++) {
			switch (cs[i]) {
			case '(':
				depth++;
				continue;
			case ')':
				if (1 == depth--) {
					return new String[] { createStringTrimmed(cs, start, ++i - start),
							createStringTrimmed(cs, i, end - i) };
				}
			}
		}
		return new String[] { new String(cs, start, offset) };
	}

	public static Class boxPrimitive(final Class cls) {
		if (cls == int.class || cls == Integer.class) {
			return Integer.class;
		} else if (cls == int[].class || cls == Integer[].class) {
			return Integer[].class;
		} else if (cls == char.class || cls == Character.class) {
			return Character.class;
		} else if (cls == char[].class || cls == Character[].class) {
			return Character[].class;
		} else if (cls == long.class || cls == Long.class) {
			return Long.class;
		} else if (cls == long[].class || cls == Long[].class) {
			return Long[].class;
		} else if (cls == short.class || cls == Short.class) {
			return Short.class;
		} else if (cls == short[].class || cls == Short[].class) {
			return Short[].class;
		} else if (cls == double.class || cls == Double.class) {
			return Double.class;
		} else if (cls == double[].class || cls == Double[].class) {
			return Double[].class;
		} else if (cls == float.class || cls == Float.class) {
			return Float.class;
		} else if (cls == float[].class || cls == Float[].class) {
			return Float[].class;
		} else if (cls == boolean.class || cls == Boolean.class) {
			return Boolean.class;
		} else if (cls == boolean[].class || cls == Boolean[].class) {
			return Boolean[].class;
		} else if (cls == byte.class || cls == Byte.class) {
			return Byte.class;
		} else if (cls == byte[].class || cls == Byte[].class) {
			return Byte[].class;
		}

		return cls;
	}

	public static Class unboxPrimitive(final Class cls) {
		if (cls == Integer.class || cls == int.class) {
			return int.class;
		} else if (cls == Integer[].class || cls == int[].class) {
			return int[].class;
		} else if (cls == Long.class || cls == long.class) {
			return long.class;
		} else if (cls == Long[].class || cls == long[].class) {
			return long[].class;
		} else if (cls == Character.class || cls == char.class) {
			return char.class;
		} else if (cls == Character[].class || cls == char[].class) {
			return char[].class;
		} else if (cls == Short.class || cls == short.class) {
			return short.class;
		} else if (cls == Short[].class || cls == short[].class) {
			return short[].class;
		} else if (cls == Double.class || cls == double.class) {
			return double.class;
		} else if (cls == Double[].class || cls == double[].class) {
			return double[].class;
		} else if (cls == Float.class || cls == float.class) {
			return float.class;
		} else if (cls == Float[].class || cls == float[].class) {
			return float[].class;
		} else if (cls == Boolean.class || cls == boolean.class) {
			return boolean.class;
		} else if (cls == Boolean[].class || cls == boolean[].class) {
			return boolean[].class;
		} else if (cls == Byte.class || cls == byte.class) {
			return byte.class;
		} else if (cls == Byte[].class || cls == byte[].class) {
			return byte[].class;
		}

		return cls;
	}

	public static boolean containsCheck(final Object compareTo, final Object compareTest) {
		if (compareTo == null) {
			return false;
		} else if (compareTo instanceof String) {
			return ((String) compareTo).contains(valueOf(compareTest));
		} else if (compareTo instanceof Collection) {
			return ((Collection) compareTo).contains(compareTest);
		} else if (compareTo instanceof Map) {
			return ((Map) compareTo).containsKey(compareTest);
		} else if (compareTo.getClass().isArray()) {
			if (compareTo.getClass().getComponentType().isPrimitive()) {
				return containsCheckOnPrimitveArray(compareTo, compareTest);
			}
			for (final Object o : ((Object[]) compareTo)) {
				if (compareTest == null && o == null) {
					return true;
				}
				if ((Boolean) MathProcessor.doOperations(o, Operator.EQUAL, compareTest)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean containsCheckOnPrimitveArray(final Object primitiveArray,
			final Object compareTest) {
		final Class<?> primitiveType = primitiveArray.getClass().getComponentType();
		if (primitiveType == boolean.class) {
			return compareTest instanceof Boolean
					&& containsCheckOnBooleanArray((boolean[]) primitiveArray, (Boolean) compareTest);
		}
		if (primitiveType == int.class) {
			return compareTest instanceof Integer
					&& containsCheckOnIntArray((int[]) primitiveArray, (Integer) compareTest);
		}
		if (primitiveType == long.class) {
			return compareTest instanceof Long
					&& containsCheckOnLongArray((long[]) primitiveArray, (Long) compareTest);
		}
		if (primitiveType == double.class) {
			return compareTest instanceof Double
					&& containsCheckOnDoubleArray((double[]) primitiveArray, (Double) compareTest);
		}
		if (primitiveType == float.class) {
			return compareTest instanceof Float
					&& containsCheckOnFloatArray((float[]) primitiveArray, (Float) compareTest);
		}
		if (primitiveType == char.class) {
			return compareTest instanceof Character
					&& containsCheckOnCharArray((char[]) primitiveArray, (Character) compareTest);
		}
		if (primitiveType == short.class) {
			return compareTest instanceof Short
					&& containsCheckOnShortArray((short[]) primitiveArray, (Short) compareTest);
		}
		if (primitiveType == byte.class) {
			return compareTest instanceof Byte
					&& containsCheckOnByteArray((byte[]) primitiveArray, (Byte) compareTest);
		}
		return false;
	}

	private static boolean containsCheckOnBooleanArray(final boolean[] array,
			final Boolean compareTest) {
		final boolean test = compareTest.booleanValue();
		for (final boolean b : array) {
			if (b == test) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsCheckOnIntArray(final int[] array, final Integer compareTest) {
		final int test = compareTest.intValue();
		for (final int i : array) {
			if (i == test) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsCheckOnLongArray(final long[] array, final Long compareTest) {
		final long test = compareTest.longValue();
		for (final long l : array) {
			if (l == test) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsCheckOnDoubleArray(final double[] array, final Double compareTest) {
		final double test = compareTest.doubleValue();
		for (final double d : array) {
			if (d == test) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsCheckOnFloatArray(final float[] array, final Float compareTest) {
		final float test = compareTest.floatValue();
		for (final float f : array) {
			if (f == test) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsCheckOnCharArray(final char[] array, final Character compareTest) {
		final char test = compareTest.charValue();
		for (final char c : array) {
			if (c == test) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsCheckOnShortArray(final short[] array, final Short compareTest) {
		final short test = compareTest.shortValue();
		for (final short s : array) {
			if (s == test) {
				return true;
			}
		}
		return false;
	}

	private static boolean containsCheckOnByteArray(final byte[] array, final Byte compareTest) {
		final byte test = compareTest.byteValue();
		for (final byte b : array) {
			if (b == test) {
				return true;
			}
		}
		return false;
	}

	public static int createClassSignatureHash(final Class declaring, final Class[] sig) {
		int hash = 0;
		for (int i = 0; i < sig.length; i++) {
			if (sig[i] != null) {
				hash += (sig[i].hashCode() * (i * 2 + 3));
			}
		}

		return hash + sig.length + declaring.hashCode();
	}

	/**
	 * Replace escape sequences and return trim required.
	 * 
	 * @param escapeStr
	 *           -
	 * @param pos
	 *           -
	 * @return -
	 */
	public static int handleEscapeSequence(final char[] escapeStr, int pos) {
		escapeStr[pos - 1] = 0;

		switch (escapeStr[pos]) {
		case '\\':
			escapeStr[pos] = '\\';
			return 1;
		case 'b':
			escapeStr[pos] = '\b';
			return 1;
		case 'f':
			escapeStr[pos] = '\f';
			return 1;
		case 't':
			escapeStr[pos] = '\t';
			return 1;
		case 'r':
			escapeStr[pos] = '\r';
			return 1;
		case 'n':
			escapeStr[pos] = '\n';
			return 1;
		case '\'':
			escapeStr[pos] = '\'';
			return 1;
		case '"':
			escapeStr[pos] = '\"';
			return 1;
		case 'u':
			// unicode
			int s = pos;
			if (s + 4 > escapeStr.length) {
				throw new CompileException("illegal unicode escape sequence", escapeStr, pos);
			} else {
				while (++pos - s != 5) {
					if ((escapeStr[pos] > ('0' - 1) && escapeStr[pos] < ('9' + 1))
							|| (escapeStr[pos] > ('A' - 1) && escapeStr[pos] < ('F' + 1))) {
					} else {
						throw new CompileException("illegal unicode escape sequence", escapeStr, pos);
					}
				}

				escapeStr[s - 1] = (char) Integer.decode("0x" + new String(escapeStr, s + 1, 4))
						.intValue();
				escapeStr[s] = 0;
				escapeStr[s + 1] = 0;
				escapeStr[s + 2] = 0;
				escapeStr[s + 3] = 0;
				escapeStr[s + 4] = 0;

				return 5;
			}

		default:
			// octal
			s = pos;
			while (escapeStr[pos] >= '0' && escapeStr[pos] < '8') {
				if (pos != s && escapeStr[s] > '3') {
					escapeStr[s - 1] = (char) Integer
							.decode("0" + new String(escapeStr, s, pos - s + 1)).intValue();
					escapeStr[s] = 0;
					escapeStr[s + 1] = 0;
					return 2;
				} else if ((pos - s) == 2) {
					escapeStr[s - 1] = (char) Integer
							.decode("0" + new String(escapeStr, s, pos - s + 1)).intValue();
					escapeStr[s] = 0;
					escapeStr[s + 1] = 0;
					escapeStr[s + 2] = 0;
					return 3;
				}

				if (pos + 1 == escapeStr.length || (escapeStr[pos] < '0' || escapeStr[pos] > '7')) {
					escapeStr[s - 1] = (char) Integer
							.decode("0" + new String(escapeStr, s, pos - s + 1)).intValue();
					escapeStr[s] = 0;
					return 1;
				}

				pos++;
			}
			throw new CompileException("illegal escape sequence: " + escapeStr[pos], escapeStr, pos);
		}
	}

	public static char[] createShortFormOperativeAssignment(final String name,
			final char[] statement, final int start, final int offset, final int operation) {
		if (operation == -1) {
			return statement;
		}

		char[] stmt;
		char op = 0;
		switch (operation) {
		case Operator.ADD:
			op = '+';
			break;
		case Operator.STR_APPEND:
			op = '#';
			break;
		case Operator.SUB:
			op = '-';
			break;
		case Operator.MULT:
			op = '*';
			break;
		case Operator.MOD:
			op = '%';
			break;
		case Operator.DIV:
			op = '/';
			break;
		case Operator.BW_AND:
			op = '&';
			break;
		case Operator.BW_OR:
			op = '|';
			break;
		case Operator.BW_SHIFT_LEFT:
			op = '\u00AB';
			break;
		case Operator.BW_SHIFT_RIGHT:
			op = '\u00BB';
			break;
		case Operator.BW_USHIFT_RIGHT:
			op = '\u00AC';
			break;
		}

		arraycopy(name.toCharArray(), 0, (stmt = new char[name.length() + offset + 1]), 0,
				name.length());
		stmt[name.length()] = op;
		arraycopy(statement, start, stmt, name.length() + 1, offset);

		return stmt;
	}

	public static ClassImportResolverFactory findClassImportResolverFactory(
			final VariableResolverFactory factory, final ParserContext pCtx) {
		if (factory == null) {
			throw new OptimizationFailure(
					"unable to import classes.  no variable resolver factory available.");
		}

		for (VariableResolverFactory v = factory; v != null; v = v.getNextFactory()) {
			if (v instanceof ClassImportResolverFactory) {
				return (ClassImportResolverFactory) v;
			}
		}

		return appendFactory(factory, new ClassImportResolverFactory(null, null, false));
	}

	public static Class findClass(final VariableResolverFactory factory, final String name,
			final ParserContext pCtx) throws ClassNotFoundException {
		try {
			if (LITERALS.containsKey(name)) {
				return (Class) LITERALS.get(name);
			} else if (factory != null && factory.isResolveable(name)) {
				return (Class) factory.getVariableResolver(name).getValue();
			} else if (pCtx != null && pCtx.hasImport(name)) {
				return pCtx.getImport(name);
			} else {
				return createClass(name, pCtx);
			}
		} catch (final ClassNotFoundException e) {
			throw e;
		} catch (final Exception e) {
			throw new RuntimeException("class not found: " + name, e);
		}
	}

	public static char[] subsetTrimmed(final char[] array, int start, int length) {
		if (length <= 0) {
			return new char[0];
		}

		int end = start + length;
		while (end > 0 && isWhitespace(array[end - 1])) {
			end--;
		}

		while (isWhitespace(array[start]) && start < end) {
			start++;
		}

		length = end - start;

		if (length == 0) {
			return new char[0];
		}

		return subset(array, start, length);
	}

	public static char[] subset(final char[] array, final int start, final int length) {

		final char[] newArray = new char[length];

		for (int i = 0; i < newArray.length; i++) {
			newArray[i] = array[i + start];
		}

		return newArray;
	}

	public static char[] subset(final char[] array, final int start) {
		final char[] newArray = new char[array.length - start];

		for (int i = 0; i < newArray.length; i++) {
			newArray[i] = array[i + start];
		}

		return newArray;
	}

	private static final HashMap<Class, Integer> typeResolveMap = new HashMap<Class, Integer>();

	static {
		final Map<Class, Integer> t = typeResolveMap;
		t.put(BigDecimal.class, DataTypes.BIG_DECIMAL);
		t.put(BigInteger.class, DataTypes.BIG_INTEGER);
		t.put(String.class, DataTypes.STRING);

		t.put(int.class, INTEGER);
		t.put(Integer.class, DataTypes.W_INTEGER);

		t.put(short.class, DataTypes.SHORT);
		t.put(Short.class, DataTypes.W_SHORT);

		t.put(float.class, DataTypes.FLOAT);
		t.put(Float.class, DataTypes.W_FLOAT);

		t.put(double.class, DOUBLE);
		t.put(Double.class, DataTypes.W_DOUBLE);

		t.put(long.class, LONG);
		t.put(Long.class, DataTypes.W_LONG);

		t.put(boolean.class, DataTypes.BOOLEAN);
		t.put(Boolean.class, DataTypes.W_BOOLEAN);

		t.put(byte.class, DataTypes.BYTE);
		t.put(Byte.class, DataTypes.W_BYTE);

		t.put(char.class, DataTypes.CHAR);
		t.put(Character.class, DataTypes.W_CHAR);

		t.put(BlankLiteral.class, DataTypes.EMPTY);
	}

	public static int resolveType(final Object o) {
		if (o == null) {
			return DataTypes.OBJECT;
		} else {
			return __resolveType(o.getClass());
		}
	}

	private static final Map<Class, Integer> typeCodes = new HashMap<Class, Integer>(30, 0.5f);

	static {
		typeCodes.put(Integer.class, DataTypes.W_INTEGER);
		typeCodes.put(Double.class, DataTypes.W_DOUBLE);
		typeCodes.put(Boolean.class, DataTypes.W_BOOLEAN);
		typeCodes.put(String.class, DataTypes.STRING);
		typeCodes.put(Long.class, DataTypes.W_LONG);
		typeCodes.put(Short.class, DataTypes.W_SHORT);
		typeCodes.put(Float.class, DataTypes.W_FLOAT);
		typeCodes.put(Byte.class, DataTypes.W_BYTE);
		typeCodes.put(Character.class, DataTypes.W_CHAR);

		typeCodes.put(BigDecimal.class, DataTypes.BIG_DECIMAL);
		typeCodes.put(BigInteger.class, DataTypes.BIG_INTEGER);

		typeCodes.put(int.class, DataTypes.INTEGER);
		typeCodes.put(double.class, DataTypes.DOUBLE);
		typeCodes.put(boolean.class, DataTypes.BOOLEAN);
		typeCodes.put(long.class, DataTypes.LONG);
		typeCodes.put(short.class, DataTypes.SHORT);
		typeCodes.put(float.class, DataTypes.FLOAT);
		typeCodes.put(byte.class, DataTypes.BYTE);
		typeCodes.put(char.class, DataTypes.CHAR);

		typeCodes.put(BlankLiteral.class, DataTypes.EMPTY);
	}

	public static int __resolveType(final Class cls) {
		final Integer code = typeCodes.get(cls);
		if (code == null) {
			if (cls != null && Collection.class.isAssignableFrom(cls)) {
				return DataTypes.COLLECTION;
			} else {
				return DataTypes.OBJECT;
			}
		}
		return code;

		// if (Integer.class == cls)
		// return DataTypes.W_INTEGER;
		// if (Double.class == cls)
		// return DataTypes.W_DOUBLE;
		// if (Boolean.class == cls)
		// return DataTypes.W_BOOLEAN;
		// if (String.class == cls)
		// return DataTypes.STRING;
		// if (Long.class == cls)
		// return DataTypes.W_LONG;
		//
		// if (Short.class == cls)
		// return DataTypes.W_SHORT;
		// if (Float.class == cls)
		// return DataTypes.W_FLOAT;
		//
		// if (Byte.class == cls)
		// return DataTypes.W_BYTE;
		// if (Character.class == cls)
		// return DataTypes.W_CHAR;
		//
		// if (BigDecimal.class == cls)
		// return DataTypes.BIG_DECIMAL;
		//
		// if (BigInteger.class == cls)
		// return DataTypes.BIG_INTEGER;
		//
		// if (int.class == cls)
		// return INTEGER;
		// if (short.class == cls)
		// return DataTypes.SHORT;
		// if (float.class == cls)
		// return DataTypes.FLOAT;
		// if (double.class == cls)
		// return DOUBLE;
		// if (long.class == cls)
		// return LONG;
		// if (boolean.class == cls)
		// return DataTypes.BOOLEAN;
		// if (byte.class == cls)
		// return DataTypes.BYTE;
		// if (char.class == cls)
		// return DataTypes.CHAR;
		//
		// if (BlankLiteral.class == cls)
		// return DataTypes.EMPTY;

		// return DataTypes.OBJECT;
	}

	public static boolean isNumericallyCoercible(final Class target, final Class parm) {
		Class boxedTarget = target.isPrimitive() ? boxPrimitive(target) : target;

		if (boxedTarget != null && Number.class.isAssignableFrom(target)) {
			if ((boxedTarget = parm.isPrimitive() ? boxPrimitive(parm) : parm) != null) {
				return Number.class.isAssignableFrom(boxedTarget);
			}
		}
		return false;
	}

	public static Object narrowType(final BigDecimal result, final int returnTarget) {
		if (returnTarget == DataTypes.W_DOUBLE || result.scale() > 0) {
			return result.doubleValue();
		} else if (returnTarget == DataTypes.W_LONG || result.longValue() > Integer.MAX_VALUE) {
			return result.longValue();
		} else {
			return result.intValue();
		}
	}

	public static Method determineActualTargetMethod(final Method method) {
		final String name = method.getName();

		/**
		 * Follow our way up the class heirarchy until we find the physical target
		 * method.
		 */
		for (final Class cls : method.getDeclaringClass().getInterfaces()) {
			for (final Method meth : cls.getMethods()) {
				if (meth.getParameterTypes().length == 0 && name.equals(meth.getName())) {
					return meth;
				}
			}
		}

		return null;
	}

	public static int captureToNextTokenJunction(final char[] expr, int cursor, final int end,
			final ParserContext pCtx) {
		while (cursor != expr.length) {
			switch (expr[cursor]) {
			case '{':
			case '(':
				return cursor;
			case '[':
				cursor = balancedCaptureWithLineAccounting(expr, cursor, end, '[', pCtx) + 1;
				continue;
			default:
				if (isWhitespace(expr[cursor])) {
					return cursor;
				}
				cursor++;
			}
		}
		return cursor;
	}

	public static int nextNonBlank(final char[] expr, final int cursor) {
		if ((cursor + 1) >= expr.length) {
			throw new CompileException("unexpected end of statement", expr, cursor);
		}
		int i = cursor;
		while (i != expr.length && isWhitespace(expr[i])) {
			i++;
		}
		return i;
	}

	public static int skipWhitespace(final char[] expr, int cursor) {
		// int line = 0;
		// int lastLineStart = 0;
		//
		// if (pCtx != null) {
		// line = pCtx.getLineCount();
		// lastLineStart = pCtx.getLineOffset();
		// }
		Skip: while (cursor != expr.length) {
			switch (expr[cursor]) {
			case '\n':
			case '\r':
				cursor++;
				continue;
			case '/':
				if (cursor + 1 != expr.length) {
					switch (expr[cursor + 1]) {
					case '/':
						expr[cursor++] = ' ';
						while (cursor != expr.length && expr[cursor] != '\n') {
							expr[cursor++] = ' ';
						}
						if (cursor != expr.length) {
							expr[cursor++] = ' ';
						}
						continue;

					case '*':
						final int len = expr.length - 1;
						expr[cursor++] = ' ';
						while (cursor != len && !(expr[cursor] == '*' && expr[cursor + 1] == '/')) {
							expr[cursor++] = ' ';
						}
						if (cursor != len) {
							expr[cursor++] = expr[cursor++] = ' ';
						}
						continue;

					default:
						break Skip;

					}
				}
			default:
				if (!isWhitespace(expr[cursor])) {
					break Skip;
				}

			}
			cursor++;
		}

		return cursor;
	}

	public static boolean isStatementNotManuallyTerminated(final char[] expr, final int cursor) {
		if (cursor >= expr.length) {
			return false;
		}
		int c = cursor;
		while (c != expr.length && isWhitespace(expr[c])) {
			c++;
		}
		return !(c != expr.length && expr[c] == ';');
	}

	public static int captureToEOS(final char[] expr, int cursor, final int end,
			final ParserContext pCtx) {
		while (cursor != expr.length) {
			switch (expr[cursor]) {
			case '(':
			case '[':
			case '{':
				if ((cursor = balancedCaptureWithLineAccounting(expr, cursor, end, expr[cursor], pCtx)) >= expr.length) {
					return cursor;
				}
				break;

			case '"':
			case '\'':
				cursor = captureStringLiteral(expr[cursor], expr, cursor, expr.length);
				break;

			case ',':
			case ';':
			case '}':
				return cursor;
			}
			cursor++;
		}
		return cursor;
	}

	/**
	 * From the specified cursor position, trim out any whitespace between the
	 * current position and the end of the last non-whitespace character.
	 * 
	 * @param expr
	 *           -
	 * @param start
	 *           -
	 * @param pos
	 *           - current position
	 * @return new position.
	 */
	public static int trimLeft(final char[] expr, final int start, int pos) {
		if (pos > expr.length) {
			pos = expr.length;
		}
		while (pos != 0 && pos >= start && isWhitespace(expr[pos - 1])) {
			pos--;
		}
		return pos;
	}

	/**
	 * From the specified cursor position, trim out any whitespace between the
	 * current position and beginning of the first non-whitespace character.
	 * 
	 * 
	 * @param expr
	 *           -
	 * @param pos
	 *           -
	 * @return -
	 */
	public static int trimRight(final char[] expr, int pos) {
		while (pos != expr.length && isWhitespace(expr[pos])) {
			pos++;
		}
		return pos;
	}

	public static char[] subArray(final char[] expr, final int start, final int end) {
		if (start >= end) {
			return new char[0];
		}

		final char[] newA = new char[end - start];
		for (int i = 0; i != newA.length; i++) {
			newA[i] = expr[i + start];
		}

		return newA;
	}

	/**
	 * This is an important aspect of the core parser tools. This method is used
	 * throughout the core parser and sub-lexical parsers to capture a balanced
	 * capture between opening and terminating tokens such as:
	 * <em>( [ { ' " </em> <br>
	 * <br>
	 * For example: ((foo + bar + (bar - foo)) * 20;<br>
	 * <br>
	 * <p/>
	 * If a balanced capture is performed from position 2, we get
	 * "(foo + bar + (bar - foo))" back.<br>
	 * If a balanced capture is performed from position 15, we get "(bar - foo)"
	 * back.<br>
	 * Etc.
	 * 
	 * @param chars
	 *           -
	 * @param start
	 *           -
	 * @param type
	 *           -
	 * @return -
	 */
	public static int balancedCapture(final char[] chars, final int start, final char type) {
		return balancedCapture(chars, start, chars.length, type);
	}

	public static int balancedCapture(final char[] chars, int start, final int end, final char type) {
		int depth = 1;
		char term = type;
		switch (type) {
		case '[':
			term = ']';
			break;
		case '{':
			term = '}';
			break;
		case '(':
			term = ')';
			break;
		}

		if (type == term) {
			for (start++; start < end; start++) {
				if (chars[start] == type) {
					return start;
				}
			}
		} else {
			for (start++; start < end; start++) {
				if (start < end && chars[start] == '/') {
					if (start + 1 == end) {
						return start;
					}
					if (chars[start + 1] == '/') {
						start++;
						while (start < end && chars[start] != '\n') {
							start++;
						}
					} else if (chars[start + 1] == '*') {
						start += 2;
						SkipComment: while (start < end) {
							switch (chars[start]) {
							case '*':
								if (start + 1 < end && chars[start + 1] == '/') {
									break SkipComment;
								}
							case '\r':
							case '\n':

								break;
							}
							start++;
						}
					}
				}
				if (start == end) {
					return start;
				}
				if (chars[start] == '\'' || chars[start] == '"') {
					start = captureStringLiteral(chars[start], chars, start, end);
				} else if (chars[start] == type) {
					depth++;
				} else if (chars[start] == term && --depth == 0) {
					return start;
				}
			}
		}

		switch (type) {
		case '[':
			throw new CompileException("unbalanced braces [ ... ]", chars, start);
		case '{':
			throw new CompileException("unbalanced braces { ... }", chars, start);
		case '(':
			throw new CompileException("unbalanced braces ( ... )", chars, start);
		default:
			throw new CompileException("unterminated string literal", chars, start);
		}
	}

	public static int balancedCaptureWithLineAccounting(final char[] chars, int start,
			final int end, final char type, final ParserContext pCtx) {
		int depth = 1;
		final int st = start;
		char term = type;
		switch (type) {
		case '[':
			term = ']';
			break;
		case '{':
			term = '}';
			break;
		case '(':
			term = ')';
			break;
		}

		if (type == term) {
			for (start++; start != end; start++) {
				if (chars[start] == type) {
					return start;
				}
			}
		} else {
			int lines = 0;
			for (start++; start < end; start++) {
				if (isWhitespace(chars[start])) {
					switch (chars[start]) {
					case '\r':
						continue;
					case '\n':
						if (pCtx != null) {
							pCtx.setLineOffset((short) start);
						}
						lines++;
					}
				} else if (start < end && chars[start] == '/') {
					if (start + 1 == end) {
						return start;
					}
					if (chars[start + 1] == '/') {
						start++;
						while (start < end && chars[start] != '\n') {
							start++;
						}
					} else if (chars[start + 1] == '*') {
						start += 2;
						Skiploop: while (start != end) {
							switch (chars[start]) {
							case '*':
								if (start + 1 < end && chars[start + 1] == '/') {
									break Skiploop;
								}
							case '\r':
							case '\n':
								if (pCtx != null) {
									pCtx.setLineOffset((short) start);
								}
								lines++;
								break;
							}
							start++;
						}
					}
				}
				if (start == end) {
					return start;
				}
				if (chars[start] == '\'' || chars[start] == '"') {
					start = captureStringLiteral(chars[start], chars, start, end);
				} else if (chars[start] == type) {
					depth++;
				} else if (chars[start] == term && --depth == 0) {
					if (pCtx != null) {
						pCtx.incrementLineCount(lines);
					}
					return start;
				}
			}
		}

		switch (type) {
		case '[':
			throw new CompileException("unbalanced braces [ ... ]", chars, st);
		case '{':
			throw new CompileException("unbalanced braces { ... }", chars, st);
		case '(':
			throw new CompileException("unbalanced braces ( ... )", chars, st);
		default:
			throw new CompileException("unterminated string literal", chars, st);
		}
	}

	public static String handleStringEscapes(final char[] input) {
		int escapes = 0;
		for (int i = 0; i < input.length; i++) {
			if (input[i] == '\\') {
				escapes += handleEscapeSequence(input, ++i);
			}
		}

		if (escapes == 0) {
			return new String(input);
		}

		final char[] processedEscapeString = new char[input.length - escapes];
		int cursor = 0;
		for (final char aName : input) {
			if (aName != 0) {
				processedEscapeString[cursor++] = aName;
			}
		}

		return new String(processedEscapeString);
	}

	public static int captureStringLiteral(final char type, final char[] expr, int cursor,
			final int end) {
		while (++cursor < end && expr[cursor] != type) {
			if (expr[cursor] == '\\') {
				cursor++;
			}
		}

		if (cursor >= end || expr[cursor] != type) {
			throw new CompileException("unterminated string literal", expr, cursor);
		}

		return cursor;
	}

	public static void parseWithExpressions(final String nestParm, final char[] block,
			final int start, final int offset, final Object ctx, final VariableResolverFactory factory) {
		/**
		 * 
		 * MAINTENANCE NOTE: A COMPILING VERSION OF THIS CODE IS DUPLICATED IN:
		 * WithNode
		 * 
		 */
		int _st = start;
		int _end = -1;

		final int end = start + offset;

		int oper = -1;
		String parm = "";

		for (int i = start; i < end; i++) {
			switch (block[i]) {
			case '{':
			case '[':
			case '(':
			case '\'':
			case '"':
				i = balancedCapture(block, i, end, block[i]);
				continue;

			case '/':
				if (i < end && block[i + 1] == '/') {
					while (i < end && block[i] != '\n') {
						block[i++] = ' ';
					}
					if (parm == null) {
						_st = i;
					}
				} else if (i < end && block[i + 1] == '*') {
					final int len = end - 1;
					while (i < len && !(block[i] == '*' && block[i + 1] == '/')) {
						block[i++] = ' ';
					}
					block[i++] = ' ';
					block[i++] = ' ';

					if (parm == null) {
						_st = i;
					}
				} else if (i < end && block[i + 1] == '=') {
					oper = Operator.DIV;
				}
				continue;

			case '%':
			case '*':
			case '-':
			case '+':
				if (i + 1 < end && block[i + 1] == '=') {
					oper = opLookup(block[i]);
				}
				continue;

			case '=':
				parm = new String(block, _st, i - _st - (oper != -1 ? 1 : 0)).trim();
				_st = i + 1;
				continue;

			case ',':
				if (_end == -1) {
					_end = i;
				}

				if (parm == null) {
					try {
						if (nestParm == null) {
							MVEL.eval(new String(block, _st, _end - _st), ctx, factory);
						} else {
							MVEL.eval(
									new StringBuilder(nestParm).append('.').append(block, _st, _end - _st)
											.toString(), ctx, factory);
						}
					} catch (final CompileException e) {
						e.setCursor(_st + (e.getCursor() - (e.getExpr().length - offset)));
						e.setExpr(block);
						throw e;
					}

					oper = -1;
					_st = ++i;
				} else {
					try {
						if (oper != -1) {
							if (nestParm == null) {
								throw new CompileException("operative assignment not possible here", block,
										start);
							}

							final String rewrittenExpr = new String(createShortFormOperativeAssignment(
									nestParm + "." + parm, block, _st, _end - _st, oper));

							MVEL.setProperty(ctx, parm, MVEL.eval(rewrittenExpr, ctx, factory));
						} else {
							MVEL.setProperty(ctx, parm, MVEL.eval(block, _st, _end - _st, ctx, factory));
						}
					} catch (final CompileException e) {
						e.setCursor(_st + (e.getCursor() - (e.getExpr().length - offset)));
						e.setExpr(block);
						throw e;
					}

					parm = null;
					oper = -1;
					_st = ++i;
				}

				_end = -1;
				break;
			}
		}

		if (_st != (_end = end)) {
			try {
				if (parm == null || "".equals(parm)) {
					if (nestParm == null) {
						MVEL.eval(new String(block, _st, _end - _st), ctx, factory);
					} else {
						MVEL.eval(new StringAppender(nestParm).append('.').append(block, _st, _end - _st)
								.toString(), ctx, factory);
					}
				} else {
					if (oper != -1) {
						if (nestParm == null) {
							throw new CompileException("operative assignment not possible here", block,
									start);
						}

						final String rewrittenExpr = new String(createShortFormOperativeAssignment(
								nestParm + "." + parm, block, _st, _end - _st, oper));

						MVEL.setProperty(ctx, parm, MVEL.eval(rewrittenExpr, ctx, factory));
					} else {
						MVEL.setProperty(ctx, parm, MVEL.eval(block, _st, end - _st, ctx, factory));
					}
				}
			} catch (final CompileException e) {
				e.setCursor(_st + (e.getCursor() - (e.getExpr().length - offset)));
				e.setExpr(block);
				throw e;
			}
		}
	}

	public static Object handleNumericConversion(final char[] val, final int start, final int offset) {
		if (offset != 1 && val[start] == '0' && val[start + 1] != '.') {
			if (!isDigit(val[start + offset - 1])) {
				switch (val[start + offset - 1]) {
				case 'L':
				case 'l':
					return Long.decode(new String(val, start, offset - 1));
				case 'I':
					return BigInteger.valueOf(Long.decode(new String(val, start, offset - 1)));
				case 'B':
					return BigDecimal.valueOf(Long.decode(new String(val, start, offset - 1)));
				}
			}

			return Integer.decode(new String(val));
		} else if (!isDigit(val[start + offset - 1])) {
			switch (val[start + offset - 1]) {
			case 'l':
			case 'L':
				return java.lang.Long.parseLong(new String(val, start, offset - 1));
			case '.':
			case 'd':
			case 'D':
				return parseDouble(new String(val, start, offset - 1));
			case 'f':
			case 'F':
				return java.lang.Float.parseFloat(new String(val, start, offset - 1));
			case 'I':
				return new BigInteger(new String(val, start, offset - 1));
			case 'B':
				return new BigDecimal(new String(val, start, offset - 1));
			}
			throw new CompileException("unrecognized numeric literal", val, start);
		} else {
			switch (numericTest(val, start, offset)) {
			case DataTypes.FLOAT:
				return java.lang.Float.parseFloat(new String(val, start, offset));
			case INTEGER:
				return java.lang.Integer.parseInt(new String(val, start, offset));
			case LONG:
				return java.lang.Long.parseLong(new String(val, start, offset));
			case DOUBLE:
				return parseDouble(new String(val, start, offset));
			case DataTypes.BIG_DECIMAL:
				return new BigDecimal(val, MathContext.DECIMAL128);
			default:
				return new String(val, start, offset);
			}
		}
	}

	public static boolean isNumeric(final Object val) {
		if (val == null) {
			return false;
		}

		Class clz;
		if (val instanceof Class) {
			clz = (Class) val;
		} else {
			clz = val.getClass();
		}

		return clz == int.class || clz == long.class || clz == short.class || clz == double.class
				|| clz == float.class || Number.class.isAssignableFrom(clz);
	}

	public static int numericTest(final char[] val, final int start, final int offset) {
		boolean fp = false;

		char c;
		int i = start;

		if (offset > 1) {
			if (val[start] == '-') {
				i++;
			} else if (val[start] == '~') {
				i++;
				if (val[start + 1] == '-') {
					i++;
				}
			}
		}

		final int end = start + offset;
		for (; i < end; i++) {
			if (!isDigit(c = val[i])) {
				switch (c) {
				case '.':
					fp = true;
					break;
				case 'e':
				case 'E':
					fp = true;
					if (i++ < end && val[i] == '-') {
						i++;
					}
					break;

				default:
					return -1;
				}
			}
		}

		if (offset != 0) {
			if (fp) {
				return DOUBLE;
			} else if (offset > 9) {
				return LONG;
			} else {
				return INTEGER;
			}
		}
		return -1;
	}

	public static boolean isNumber(final Object val) {
		if (val == null) {
			return false;
		}
		if (val instanceof String) {
			return isNumber((String) val);
		}
		if (val instanceof char[]) {
			return isNumber(new String((char[]) val));
		}
		return val instanceof Integer || val instanceof BigDecimal || val instanceof BigInteger
				|| val instanceof Float || val instanceof Double || val instanceof Long
				|| val instanceof Short || val instanceof Character;
	}

	public static boolean isNumber(final String val) {
		final int len = val.length();
		char c;
		boolean f = true;
		int i = 0;
		if (len > 1) {
			if (val.charAt(0) == '-') {
				i++;
			} else if (val.charAt(0) == '~') {
				i++;
				if (val.charAt(1) == '-') {
					i++;
				}
			}
		}
		for (; i < len; i++) {
			if (!isDigit(c = val.charAt(i))) {
				if (c == '.' && f) {
					f = false;
				} else {
					return false;
				}
			}
		}

		return len > 0;
	}

	public static boolean isNumber(final char[] val, final int start, final int offset) {
		char c;
		boolean f = true;
		int i = start;
		final int end = start + offset;
		if (offset > 1) {
			switch (val[start]) {
			case '-':
				if (val[start + 1] == '-') {
					i++;
				}
			case '~':
				i++;
			}
		}
		for (; i < end; i++) {
			if (!isDigit(c = val[i])) {
				if (f && c == '.') {
					f = false;
				} else if (offset != 1 && i == start + offset - 1) {
					switch (c) {
					case 'l':
					case 'L':
					case 'f':
					case 'F':
					case 'd':
					case 'D':
					case 'I':
					case 'B':
						return true;
					case '.':
						throw new CompileException("invalid number literal: " + new String(val), val,
								start);
					}
					return false;
				} else if (i == start + 1 && c == 'x' && val[start] == '0') {
					for (i++; i < end; i++) {
						if (!isDigit(c = val[i])) {
							if ((c < 'A' || c > 'F') && (c < 'a' || c > 'f')) {
								if (i == offset - 1) {
									switch (c) {
									case 'l':
									case 'L':
									case 'I':
									case 'B':
										return true;
									}
								}

								return false;
							}
						}
					}
					return offset - 2 > 0;

				} else if (i != start && (i + 1) < end && (c == 'E' || c == 'e')) {
					if (val[++i] == '-' || val[i] == '+') {
						i++;
					}
				} else {
					if (i != start) {
						throw new CompileException("invalid number literal: "
								+ new String(val, start, offset), val, start);
					}
					return false;
				}
			}
		}

		return end > start;
	}

	public static int find(final char[] c, final int start, final int offset, final char find) {
		final int length = start + offset;
		for (int i = start; i < length; i++) {
			if (c[i] == find) {
				return i;
			}
		}
		return -1;
	}

	public static int findLast(final char[] c, final int start, final int offset, final char find) {
		for (int i = start + offset; i >= start; i--) {
			if (c[i] == find) {
				return i;
			}
		}
		return -1;
	}

	public static String createStringTrimmed(final char[] s) {
		int start = 0, end = s.length;
		while (start != end && s[start] < '\u0020' + 1) {
			start++;
		}
		while (end != start && s[end - 1] < '\u0020' + 1) {
			end--;
		}
		return new String(s, start, end - start);
	}

	public static String createStringTrimmed(final char[] s, int start, int length) {
		if ((length = start + length) > s.length) {
			return new String(s);
		}
		while (start != length && s[start] < '\u0020' + 1) {
			start++;
		}
		while (length != start && s[length - 1] < '\u0020' + 1) {
			length--;
		}
		return new String(s, start, length - start);
	}

	public static boolean endsWith(final char[] c, final int start, final int offset,
			final char[] test) {
		if (test.length > c.length) {
			return false;
		}

		int tD = test.length - 1;
		int cD = start + offset - 1;

		while (tD >= 0) {
			if (c[cD--] != test[tD--]) {
				return false;
			}
		}

		return true;
	}

	public static boolean isIdentifierPart(final int c) {
		return ((c > 96 && c < 123) || (c > 64 && c < 91) || (c > 47 && c < 58) || (c == '_')
				|| (c == '$') || Character.isJavaIdentifierPart(c));
	}

	public static boolean isDigit(final int c) {
		return c > ('0' - 1) && c < ('9' + 1);
	}

	public static float similarity(final String s1, final String s2) {
		if (s1 == null || s2 == null) {
			return s1 == null && s2 == null ? 1f : 0f;
		}

		final char[] c1 = s1.toCharArray();
		final char[] c2 = s2.toCharArray();

		char[] comp;
		char[] against;

		float same = 0;
		float baselength;

		int cur1 = 0;

		if (c1.length > c2.length) {
			baselength = c1.length;
			comp = c1;
			against = c2;
		} else {
			baselength = c2.length;
			comp = c2;
			against = c1;
		}

		while (cur1 < comp.length && cur1 < against.length) {
			if (comp[cur1] == against[cur1]) {
				same++;
			}

			cur1++;
		}

		return same / baselength;
	}

	public static int findAbsoluteLast(final char[] array) {
		int depth = 0;
		for (int i = array.length - 1; i >= 0; i--) {
			if (array[i] == ']') {
				depth++;
			}
			if (array[i] == '[') {
				depth--;
			}

			if (depth == 0 && array[i] == '.' || array[i] == '[') {
				return i;
			}
		}
		return -1;
	}

	public static Class getBaseComponentType(Class cls) {
		while (cls.isArray()) {
			cls = cls.getComponentType();
		}
		return cls;
	}

	public static Class getSubComponentType(Class cls) {
		if (cls.isArray()) {
			cls = cls.getComponentType();
		}
		return cls;
	}

	public static boolean isJunct(final char c) {
		switch (c) {
		case '[':
		case '(':
			return true;
		default:
			return isWhitespace(c);
		}
	}

	public static int opLookup(final char c) {
		switch (c) {
		case '|':
			return Operator.BW_OR;
		case '&':
			return Operator.BW_AND;
		case '^':
			return Operator.BW_XOR;
		case '*':
			return Operator.MULT;
		case '/':
			return Operator.DIV;
		case '+':
			return Operator.ADD;
		case '%':
			return Operator.MOD;
		case '\u00AB':
			return Operator.BW_SHIFT_LEFT;
		case '\u00BB':
			return Operator.BW_SHIFT_RIGHT;
		case '\u00AC':
			return Operator.BW_USHIFT_RIGHT;
		}
		return -1;
	}

	/**
	 * Check if the specified string is a reserved word in the parser.
	 * 
	 * @param name
	 *           -
	 * @return -
	 */
	public static boolean isReservedWord(final String name) {
		return LITERALS.containsKey(name) || AbstractParser.OPERATORS.containsKey(name);
	}

	/**
	 * Check if the specfied string represents a valid name of label.
	 * 
	 * @param name
	 *           -
	 * @return -
	 */
	public static boolean isNotValidNameorLabel(final String name) {
		for (final char c : name.toCharArray()) {
			if (c == '.') {
				return true;
			} else if (!isIdentifierPart(c)) {
				return true;
			}
		}
		return false;
	}

	public static boolean isPropertyOnly(final char[] array, final int start, final int end) {
		for (int i = start; i < end; i++) {
			if (!isIdentifierPart(array[i])) {
				return false;
			}
		}
		return true;
	}

	public static boolean isArrayType(final char[] array, final int start, final int end) {
		return end > start + 2 && isPropertyOnly(array, start, end - 2) && array[end - 2] == '['
				&& array[end - 1] == ']';
	}

	public static void checkNameSafety(final String name) {
		if (isReservedWord(name)) {
			throw new RuntimeException("illegal use of reserved word: " + name);
		} else if (isDigit(name.charAt(0))) {
			throw new RuntimeException("not an identifier: " + name);
		}
	}

	public static FileWriter getDebugFileWriter() throws IOException {
		return new FileWriter(new File(getDebuggingOutputFileName()), true);
	}

	public static boolean isPrimitiveWrapper(final Class clazz) {
		return clazz == Integer.class || clazz == Boolean.class || clazz == Long.class
				|| clazz == Double.class || clazz == Float.class || clazz == Character.class
				|| clazz == Short.class || clazz == Byte.class;
	}

	public static Serializable subCompileExpression(final char[] expression) {
		return _optimizeTree(new ExpressionCompiler(expression)._compile());
	}

	public static Serializable subCompileExpression(final char[] expression, final ParserContext ctx) {
		final ExpressionCompiler c = new ExpressionCompiler(expression);
		if (ctx != null) {
			c.setPCtx(ctx);
		}
		return _optimizeTree(c._compile());
	}

	public static Serializable subCompileExpression(final char[] expression, final int start,
			final int offset) {
		return _optimizeTree(new ExpressionCompiler(expression, start, offset)._compile());
	}

	public static Serializable subCompileExpression(final char[] expression, final int start,
			final int offset, final ParserContext ctx) {
		final ExpressionCompiler c = new ExpressionCompiler(expression, start, offset);
		if (ctx != null) {
			c.setPCtx(ctx);
		}
		return _optimizeTree(c._compile());
	}

	public static Serializable subCompileExpression(final String expression, final ParserContext ctx) {
		final ExpressionCompiler c = new ExpressionCompiler(expression);
		c.setPCtx(ctx);

		return _optimizeTree(c._compile());
	}

	public static Serializable optimizeTree(final CompiledExpression compiled) {
		/**
		 * If there is only one token, and it's an identifier, we can optimize
		 * this as an accessor expression.
		 */
		if (!compiled.isImportInjectionRequired()
				&& compiled.getParserConfiguration().isAllowBootstrapBypass()
				&& compiled.isSingleNode()) {

			return _optimizeTree(compiled);
		}

		return compiled;
	}

	private static Serializable _optimizeTree(final CompiledExpression compiled) {
		/**
		 * If there is only one token, and it's an identifier, we can optimize
		 * this as an accessor expression.
		 */
		if (compiled.isSingleNode()) {
			final ASTNode tk = compiled.getFirstNode();

			if (tk.isLiteral() && !tk.isThisVal()) {
				return new ExecutableLiteral(tk.getLiteralValue());
			}
			return tk.canSerializeAccessor() ? new ExecutableAccessorSafe(tk,
					compiled.getKnownEgressType()) : new ExecutableAccessor(tk,
					compiled.getKnownEgressType());
		}

		return compiled;
	}

	public static boolean isWhitespace(final char c) {
		return c < '\u0020' + 1;
	}

	public static String repeatChar(final char c, final int times) {
		final char[] n = new char[times];
		for (int i = 0; i < times; i++) {
			n[i] = c;
		}
		return new String(n);
	}

	public static char[] loadFromFile(final File file) throws IOException {
		return loadFromFile(file, null);
	}

	public static char[] loadFromFile(final File file, final String encoding) throws IOException {
		if (!file.exists()) {
			throw new RuntimeException("cannot find file: " + file.getName());
		}

		FileInputStream inStream = null;
		ReadableByteChannel fc = null;
		try {
			fc = (inStream = new FileInputStream(file)).getChannel();
			final ByteBuffer buf = allocateDirect(10);

			final StringAppender sb = new StringAppender((int) file.length(), encoding);

			int read = 0;
			while (read >= 0) {
				buf.rewind();
				read = fc.read(buf);
				buf.rewind();

				for (; read > 0; read--) {
					sb.append(buf.get());
				}
			}

			// noinspection unchecked
			return sb.toChars();
		} catch (final FileNotFoundException e) {
			// this can't be thrown, we check for this explicitly.
		} finally {
			if (inStream != null) {
				inStream.close();
			}
			if (fc != null) {
				fc.close();
			}
		}

		return null;
	}

	public static char[] readIn(final InputStream inStream, final String encoding)
			throws IOException {
		try {
			final byte[] buf = new byte[10];

			final StringAppender sb = new StringAppender(10, encoding);

			int bytesRead;
			while ((bytesRead = inStream.read(buf)) > 0) {
				for (int i = 0; i < bytesRead; i++) {
					sb.append(buf[i]);
				}
			}

			// noinspection unchecked
			return sb.toChars();
		} finally {
			if (inStream != null) {
				inStream.close();
			}
		}
	}
}
