package net.simpleframework.common;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class NumberUtils {
	private static Map<String, DecimalFormat> decimalFormats = new ConcurrentHashMap<String, DecimalFormat>();

	public static String formatDouble(final Number number) {
		return format(number, "#.##");
	}

	public static String format(final Number number, final String pattern) {
		if (number == null) {
			return "0";
		}
		if (!StringUtils.hasText(pattern)) {
			return number.toString();
		}
		DecimalFormat formatter = decimalFormats.get(pattern);
		if (formatter == null) {
			decimalFormats.put(pattern, formatter = new DecimalFormat(pattern));
		}
		return formatter.format(number);
	}

	public static long randomLong(final long min, final long max) {
		return min + (long) (Math.random() * (max - min));
	}

	public static int randomInt(final int min, final int max) {
		return min + (int) (Math.random() * (max - min));
	}
}
