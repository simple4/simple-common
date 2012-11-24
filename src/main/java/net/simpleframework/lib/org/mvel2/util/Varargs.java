package net.simpleframework.lib.org.mvel2.util;

import java.lang.reflect.Array;

public class Varargs {

	public static Object[] normalizeArgsForVarArgs(final Class<?>[] parameterTypes,
			final Object[] args, final boolean isVarArgs) {
		if (!isVarArgs) {
			return args;
		}
		if (parameterTypes.length == args.length && args[args.length - 1].getClass().isArray()) {
			return args;
		}

		final int varargLength = args.length - parameterTypes.length + 1;
		final Object vararg = Array.newInstance(
				parameterTypes[parameterTypes.length - 1].getComponentType(), varargLength);
		for (int i = 0; i < varargLength; i++) {
			Array.set(vararg, i, args[parameterTypes.length - 1 + i]);
		}

		final Object[] normalizedArgs = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length - 1; i++) {
			normalizedArgs[i] = args[i];
		}
		normalizedArgs[parameterTypes.length - 1] = vararg;
		return normalizedArgs;
	}

	public static Class<?> paramTypeVarArgsSafe(final Class<?>[] parameterTypes, final int i,
			final boolean isVarArgs) {
		if (!isVarArgs) {
			return parameterTypes[i];
		}
		if (i < parameterTypes.length - 1) {
			return parameterTypes[i];
		}
		return parameterTypes[parameterTypes.length - 1].getComponentType();
	}
}
