package net.simpleframework.lib.org.jsoup.helper;

import java.util.Collection;
import java.util.Iterator;

/**
 * A minimal String utility class. Designed for internal jsoup use only.
 */
public final class StringUtil {
	// memoised padding up to 10
	private static final String[] padding = { "", " ", "  ", "   ", "    ", "     ", "      ",
			"       ", "        ", "         ", "          " };

	/**
	 * Join a collection of strings by a seperator
	 * 
	 * @param strings
	 *           collection of string objects
	 * @param sep
	 *           string to place between strings
	 * @return joined string
	 */
	public static String join(final Collection strings, final String sep) {
		return join(strings.iterator(), sep);
	}

	/**
	 * Join a collection of strings by a seperator
	 * 
	 * @param strings
	 *           iterator of string objects
	 * @param sep
	 *           string to place between strings
	 * @return joined string
	 */
	public static String join(final Iterator strings, final String sep) {
		if (!strings.hasNext()) {
			return "";
		}

		final String start = strings.next().toString();
		if (!strings.hasNext()) {
			return start;
		}

		final StringBuilder sb = new StringBuilder(64).append(start);
		while (strings.hasNext()) {
			sb.append(sep);
			sb.append(strings.next());
		}
		return sb.toString();
	}

	/**
	 * Returns space padding
	 * 
	 * @param width
	 *           amount of padding desired
	 * @return string of spaces * width
	 */
	public static String padding(final int width) {
		if (width < 0) {
			throw new IllegalArgumentException("width must be > 0");
		}

		if (width < padding.length) {
			return padding[width];
		}

		final char[] out = new char[width];
		for (int i = 0; i < width; i++) {
			out[i] = ' ';
		}
		return String.valueOf(out);
	}

	/**
	 * Tests if a string is blank: null, emtpy, or only whitespace (" ", \r\n,
	 * \t, etc)
	 * 
	 * @param string
	 *           string to test
	 * @return if string is blank
	 */
	public static boolean isBlank(final String string) {
		if (string == null || string.length() == 0) {
			return true;
		}

		final int l = string.length();
		for (int i = 0; i < l; i++) {
			if (!StringUtil.isWhitespace(string.codePointAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tests if a string is numeric, i.e. contains only digit characters
	 * 
	 * @param string
	 *           string to test
	 * @return true if only digit chars, false if empty or null or contains
	 *         non-digit chrs
	 */
	public static boolean isNumeric(final String string) {
		if (string == null || string.length() == 0) {
			return false;
		}

		final int l = string.length();
		for (int i = 0; i < l; i++) {
			if (!Character.isDigit(string.codePointAt(i))) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Tests if a code point is "whitespace" as defined in the HTML spec.
	 * 
	 * @param c
	 *           code point to test
	 * @return true if code point is whitespace, false otherwise
	 */
	public static boolean isWhitespace(final int c) {
		return c == ' ' || c == '\t' || c == '\n' || c == '\f' || c == '\r';
	}

	public static String normaliseWhitespace(final String string) {
		final StringBuilder sb = new StringBuilder(string.length());

		boolean lastWasWhite = false;
		boolean modified = false;

		final int l = string.length();
		int c;
		for (int i = 0; i < l; i += Character.charCount(c)) {
			c = string.codePointAt(i);
			if (isWhitespace(c)) {
				if (lastWasWhite) {
					modified = true;
					continue;
				}
				if (c != ' ') {
					modified = true;
				}
				sb.append(' ');
				lastWasWhite = true;
			} else {
				sb.appendCodePoint(c);
				lastWasWhite = false;
			}
		}
		return modified ? sb.toString() : string;
	}

	public static boolean in(final String needle, final String... haystack) {
		for (final String hay : haystack) {
			if (hay.equals(needle)) {
				return true;
			}
		}
		return false;
	}
}
