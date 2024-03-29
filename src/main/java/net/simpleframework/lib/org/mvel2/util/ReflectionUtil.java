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

import static java.lang.System.arraycopy;

/**
 * Utilities for working with reflection.
 */
public class ReflectionUtil {
	private static final int CASE_OFFSET = ('z' - 'Z');

	/**
	 * This new method 'slightly' outperforms the old method, it was essentially
	 * a perfect example of me wasting my time and a premature optimization. But
	 * what the hell...
	 * 
	 * @param s
	 *           -
	 * @return String
	 */
	public static String getSetter(final String s) {
		final char[] chars = new char[s.length() + 3];

		chars[0] = 's';
		chars[1] = 'e';
		chars[2] = 't';

		if (s.charAt(0) > 'Z') {
			chars[3] = (char) (s.charAt(0) - CASE_OFFSET);
		} else {
			chars[3] = s.charAt(0);
		}

		for (int i = s.length() - 1; i != 0; i--) {
			chars[i + 3] = s.charAt(i);
		}

		return new String(chars);
	}

	public static String getGetter(final String s) {
		final char[] c = s.toCharArray();
		final char[] chars = new char[c.length + 3];

		chars[0] = 'g';
		chars[1] = 'e';
		chars[2] = 't';

		if (c[0] > 'Z') {
			chars[3] = (char) (c[0] - CASE_OFFSET);
		} else {
			chars[3] = (c[0]);
		}

		arraycopy(c, 1, chars, 4, c.length - 1);

		return new String(chars);
	}

	public static String getIsGetter(final String s) {
		final char[] c = s.toCharArray();
		final char[] chars = new char[c.length + 2];

		chars[0] = 'i';
		chars[1] = 's';

		if (c[0] > 'Z') {
			chars[2] = (char) (c[0] - CASE_OFFSET);
		} else {
			chars[2] = c[0];
		}

		arraycopy(c, 1, chars, 3, c.length - 1);

		return new String(chars);
	}

	public static String getPropertyFromAccessor(final String s) {
		final char[] c = s.toCharArray();
		char[] chars;

		if (c.length > 3 && c[1] == 'e' && c[2] == 't') {
			chars = new char[c.length - 3];

			if (c[0] == 'g' || c[0] == 's') {
				if (c[3] < 'a') {
					chars[0] = (char) (c[3] + CASE_OFFSET);
				} else {
					chars[0] = c[3];
				}

				for (int i = 1; i < chars.length; i++) {
					chars[i] = c[i + 3];
				}

				return new String(chars);
			} else {
				return s;
			}
		} else if (c.length > 2 && c[0] == 'i' && c[1] == 's') {
			chars = new char[c.length - 2];

			if (c[2] < 'a') {
				chars[0] = (char) (c[2] + CASE_OFFSET);
			} else {
				chars[0] = c[2];
			}

			for (int i = 1; i < chars.length; i++) {
				chars[i] = c[i + 2];
			}

			return new String(chars);
		}
		return s;
	}

	public static Class<?> toNonPrimitiveType(final Class<?> c) {
		if (!c.isPrimitive()) {
			return c;
		}
		if (c == int.class) {
			return Integer.class;
		}
		if (c == long.class) {
			return Long.class;
		}
		if (c == double.class) {
			return Double.class;
		}
		if (c == float.class) {
			return Float.class;
		}
		if (c == short.class) {
			return Short.class;
		}
		if (c == byte.class) {
			return Byte.class;
		}
		if (c == char.class) {
			return Character.class;
		}
		return Boolean.class;
	}

	public static Class<?> toNonPrimitiveArray(final Class<?> c) {
		if (!c.isArray() || !c.getComponentType().isPrimitive()) {
			return c;
		}
		if (c == int[].class) {
			return Integer[].class;
		}
		if (c == long[].class) {
			return Long[].class;
		}
		if (c == double[].class) {
			return Double[].class;
		}
		if (c == float[].class) {
			return Float[].class;
		}
		if (c == short[].class) {
			return Short[].class;
		}
		if (c == byte[].class) {
			return Byte[].class;
		}
		if (c == char[].class) {
			return Character[].class;
		}
		return Boolean[].class;
	}

	public static Class<?> toPrimitiveArrayType(final Class<?> c) {
		if (!c.isPrimitive()) {
			throw new RuntimeException(c + " is not a primitive type");
		}
		if (c == int.class) {
			return int[].class;
		}
		if (c == long.class) {
			return long[].class;
		}
		if (c == double.class) {
			return double[].class;
		}
		if (c == float.class) {
			return float[].class;
		}
		if (c == short.class) {
			return short[].class;
		}
		if (c == byte.class) {
			return byte[].class;
		}
		if (c == char.class) {
			return char[].class;
		}
		return boolean[].class;
	}

	public static boolean isAssignableFrom(final Class<?> from, final Class<?> to) {
		return from.isAssignableFrom(to) || areBoxingCompatible(from, to);
	}

	private static boolean areBoxingCompatible(final Class<?> c1, final Class<?> c2) {
		return c1.isPrimitive() ? isPrimitiveOf(c2, c1) : (c2.isPrimitive() ? isPrimitiveOf(c1, c2)
				: false);
	}

	private static boolean isPrimitiveOf(final Class<?> boxed, final Class<?> primitive) {
		if (primitive == int.class) {
			return boxed == Integer.class;
		}
		if (primitive == long.class) {
			return boxed == Long.class;
		}
		if (primitive == double.class) {
			return boxed == Double.class;
		}
		if (primitive == float.class) {
			return boxed == Float.class;
		}
		if (primitive == short.class) {
			return boxed == Short.class;
		}
		if (primitive == byte.class) {
			return boxed == Byte.class;
		}
		if (primitive == char.class) {
			return boxed == Character.class;
		}
		if (primitive == boolean.class) {
			return boxed == Boolean.class;
		}
		return false;
	}
}
