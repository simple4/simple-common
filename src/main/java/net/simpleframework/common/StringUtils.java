package net.simpleframework.common;

import java.util.Arrays;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class StringUtils {

	public static String replace(final String s, final String sub, final String with) {
		int c = 0;
		int i = s.indexOf(sub, c);
		if (i == -1) {
			return s;
		}
		final int length = s.length();
		final StringBuilder sb = new StringBuilder(length + with.length());
		do {
			sb.append(s.substring(c, i));
			sb.append(with);
			c = i + sub.length();
		} while ((i = s.indexOf(sub, c)) != -1);
		if (c < length) {
			sb.append(s.substring(c, length));
		}
		return sb.toString();
	}

	private static final String defaultDelimiter = ";";

	public static String[] split(final String src, final String delim) {
		if (src == null) {
			return null;
		}

		final int maxparts = (src.length() / delim.length()) + 2;
		final int[] positions = new int[maxparts];
		final int dellen = delim.length();

		int i, j = 0;
		int count = 0;
		positions[0] = -dellen;
		while ((i = src.indexOf(delim, j)) != -1) {
			count++;
			positions[count] = i;
			j = i + dellen;
		}
		count++;
		positions[count] = src.length();

		final String[] result = new String[count];

		for (i = 0; i < count; i++) {
			result[i] = src.substring(positions[i] + dellen, positions[i + 1]);
		}
		return result;
	}

	public static String[] split(final String str) {
		return split(str, defaultDelimiter);
	}

	public static String join(final Iterable<?> it, final String delim) {
		if (it == null) {
			return null;
		}
		final StringBuilder sb = new StringBuilder();
		for (final Object o : it) {
			if (o == null) {
				continue;
			}
			if (sb.length() > 0) {
				if (delim != null) {
					sb.append(delim);
				}
			}
			sb.append(o);
		}
		return sb.toString();
	}

	public static String join(final Object[] arr, final String delim) {
		return join(Arrays.asList(arr), delim);
	}

	public static String join(final Iterable<?> it) {
		return join(it, defaultDelimiter);
	}

	public static boolean isBlank(final CharSequence cs) {
		int strLen;
		if (cs == null || (strLen = cs.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(cs.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	}

	public static boolean hasText(final CharSequence cs) {
		return !isBlank(cs);
	}

	public static boolean hasTextObject(final Object object) {
		if (object == null) {
			return false;
		}
		if (object instanceof String) {
			return hasText((String) object);
		}
		return true;
	}

	public static String text(final String... strings) {
		if (strings != null) {
			for (final String string : strings) {
				if (hasText(string)) {
					return string;
				}
			}
		}
		return "";
	}

	public static String blank(final Object object) {
		return object == null ? "" : Convert.toString(object);
	}

	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A',
			'B', 'C', 'D', 'E', 'F' };

	public static String encodeHex(final byte[] binaryData) {
		final int l = binaryData.length;
		final char[] out = new char[l << 1];
		for (int i = 0, j = 0; i < l; i++) {
			out[j++] = DIGITS[(0xF0 & binaryData[i]) >>> 4];
			out[j++] = DIGITS[0x0F & binaryData[i]];
		}
		return new String(out);
	}

	public static byte[] decodeHex(final String encoded) {
		final char[] data = encoded.toCharArray();
		final int len = data.length;
		final byte[] out = new byte[len >> 1];
		for (int i = 0, j = 0; j < len; i++) {
			int f = Character.digit(data[j], 16) << 4;
			j++;
			f = f | Character.digit(data[j], 16);
			j++;
			out[i] = (byte) (f & 0xFF);
		}
		return out;
	}

	public static String decodeHexString(final String s) {
		return new String(decodeHex(s));
	}

	public static String hash(final Object object) {
		if (object == null) {
			return null;
		}
		final int hash = object.hashCode();
		return hash > 0 ? String.valueOf(hash) : "0" + Math.abs(hash);
	}

	public static String substring(final String str, final int length) {
		return substring(str, length, false);
	}

	public static String substring(String str, final int length, final boolean dot) {
		str = blank(str).trim();
		if (length >= str.length()) {
			return str;
		} else {
			str = str.substring(0, length);
			if (dot) {
				str += "...";
			}
			return str;
		}
	}

	public static String trimOneLine(String str) {
		for (final String c : new String[] { "\n", "\r", "\t" }) {
			str = replace(str, c, "");
		}
		return str.trim();
	}

	// file string

	private static final char UNIX_SEPARATOR = '/';

	private static final char WINDOWS_SEPARATOR = '\\';

	public static String getFilename(final String filename) {
		if (filename == null) {
			return null;
		}
		final int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
		final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
		final int index = Math.max(lastUnixPos, lastWindowsPos);
		return filename.substring(index + 1);
	}

	public static String getFilenameExtension(final String filename) {
		final int index = filename.lastIndexOf('.');
		if (-1 == index) {
			return "";
		} else {
			return filename.substring(index + 1);
		}
	}

	public static String stripFilenameExtension(final String path) {
		if (path == null) {
			return null;
		}
		final int sepIndex = path.lastIndexOf(".");
		return (sepIndex != -1 ? path.substring(0, sepIndex) : path);
	}
}
