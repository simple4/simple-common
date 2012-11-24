package net.simpleframework.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import net.simpleframework.lib.org.mvel2.DataConversion;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class Convert {
	public static <T> T convert(final Object value, final Class<T> clazz) {
		return convert(value, clazz, null);
	}

	@SuppressWarnings("unchecked")
	public static <T> T convert(final Object value, final Class<T> targetClass, final T defaultValue) {
		if (value == null) {
			return defaultValue != null ? defaultValue : null;
		}
		if (targetClass.equals(value.getClass())) {
			return (T) value;
		}

		if (Date.class.isAssignableFrom(targetClass)) {
			for (final String pattern : new String[] { defaultDatePattern, "yyyy-MM-dd",
					"yyyy-MM-dd HH:mm:ss" }) {
				final Date d = toDate(String.valueOf(value), pattern);
				if (d != null) {
					return (T) d;
				}
			}
		}

		T t = null;
		try {
			t = DataConversion.convert(value, targetClass);
		} catch (final Exception e) {
		}
		if ((t == null || value == t) && defaultValue != null) {
			return defaultValue;
		}
		return t;
	}

	public static final boolean toBool(final Object obj, final boolean defaultValue) {
		return convert(obj, Boolean.class, defaultValue);
	}

	public static final Boolean toBool(final Object obj) {
		return toBool(obj, false);
	}

	public static final byte toByte(final Object obj, final byte defaultValue) {
		return convert(obj, Byte.class, defaultValue);
	}

	public static final Byte toByte(final Object obj) {
		return toByte(obj, (byte) 0);
	}

	public static final short toShort(final Object obj, final short defaultValue) {
		return convert(obj, Short.class, defaultValue);
	}

	public static final Short toShort(final Object obj) {
		return toShort(obj, (short) 0);
	}

	public static final int toInt(final Object obj, final int defaultValue) {
		return convert(obj, Integer.class, defaultValue);
	}

	public static final Integer toInt(final Object obj) {
		return toInt(obj, 0);
	}

	public static final long toLong(final Object obj, final long defaultValue) {
		return convert(obj, Long.class, defaultValue);
	}

	public static final Long toLong(final Object obj) {
		return toLong(obj, 0l);
	}

	public static final double toDouble(final Object obj, final double defaultValue) {
		return convert(obj, Double.class, defaultValue);
	}

	public static final Double toDouble(final Object obj) {
		return toDouble(obj, 0d);
	}

	public static <T extends Enum<T>> T toEnum(final Class<T> enumClazz, final Object obj) {
		try {
			return Enum.valueOf(enumClazz, toString(obj));
		} catch (final Throwable e) {
			return enumClazz.getEnumConstants()[toInt(obj)];
		}
	}

	public static final String toString(final Object obj) {
		if (obj instanceof Throwable) {
			final StringWriter writer = new StringWriter();
			((Throwable) obj).printStackTrace(new PrintWriter(writer));
			return writer.toString();
		} else if (obj instanceof char[]) {
			return String.valueOf((char[]) obj);
		} else if (obj instanceof Properties) {
			return toString((Properties) obj, null);
		} else if (obj instanceof Iterable<?>) {
			return StringUtils.join((Iterable<?>) obj, "\n");
		} else {
			return convert(obj, String.class);
		}
	}

	public static final String toString(final Properties properties, final String header) {
		if (properties == null) {
			return null;
		}
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			properties.store(out, header);
			return new String(out.toByteArray());
		} catch (final Exception e) {
			return null;
		}
	}

	public static final String toString(final Object obj, final String defaultValue) {
		final String s = toString(obj);
		return s == null ? defaultValue : s;
	}

	public static Properties toProperties(final String properties) {
		Properties props = null;
		if (StringUtils.hasText(properties)) {
			props = new Properties();
			try {
				props.load(new ByteArrayInputStream(properties.getBytes()));
			} catch (final IOException e) {
			}
		}
		return props;
	}

	// String.format("%tb", date);
	// 常见日期格式化转换符
	// %te 一个月中的某一天（1～31 ------------------2
	// %tb 指定语言环境的月份简称 --------------------Feb（英文）、二月（中文）
	// %tB 指定语言环境的月份全称 --------------------February（英文）、二月（中文）
	// %tA 指定语言环境的星期几全称 -------------------Monday（英文）、星期一（中文）
	// %ta 指定语言环境的星期几简称 -------------------Mon（英文）、星期一（中文）
	// %tc 包括全部日期和时间信息 --------------------星期四 十一月 26 10:26:30 CST 2009
	// %tY 4位年份 -----------------------------2009
	// %tj 一年中的第几天 ------------------------（001～366） 085
	// %tm 月份 -------------------------------03
	// %td 一个月中的第几天 -----------------------（01～31） 08
	// %ty 2位年份 -----------------------------09

	// 时间格式化转换符
	// %tH 2位数字的24小时制的小时------------------（00～23） 14
	// %tI 2位数字的12小时制的小时------------------（01～12） 05
	// %tk 2位数字的24小时制的小时------------------（1～23） 5
	// %tl 2位数字的12小时制的小时------------------（1～12） 10
	// %tM 2位数字的分钟-------------------------（00～59） 05
	// %tS 2位数字的秒数-------------------------（00～60） 12
	// %tL 3位数字的毫秒数------------------------（000～999） 920
	// %tN 9位数字的微秒数------------------------（000000000～999999999） 062000000000
	// %tp 指定语言环境下上午或下午标记 ----------------下午（中文）、pm（英文）
	// %tz 相对于GMT RFC 82格式的数字时区偏移量--------+0800
	// %tZ 时区缩写形式的字符串 ----------------------CST
	// %ts 1970-01-01 00:00:00至现在经过的秒数 ------1206426646
	// %tQ 1970-01-01 00:00:00至现在经过的毫秒数 -----1206426737453

	// 常见的日期时间组合转换符
	// %tF “年-月-日”格式（4位年份）-------------------2009-01-26
	// %tD “月/日/年”格式（2位年份）-------------------03/25/09
	// %tr “时：分：秒 PM（AM）”格式（12小时制）-----------03:22:06 下午
	// %tT “时：分：秒”格式（24小时制）------------------15:23:50
	// %tR “时：分”格式（24小时制）--------------------15:23

	public static String defaultDatePattern = "yyyy-MM-dd HH:mm";

	public static final String toDateString(final Date date, final String pattern) {
		final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return date == null ? null : sdf.format(date);
	}

	public static final String toDateString(final Date date) {
		return toDateString(date, defaultDatePattern);
	}

	public static final Date toDate(final String dateString, final String pattern) {
		final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		try {
			return sdf.parse(dateString);
		} catch (final Exception e) {
			return null;
		}
	}
}
