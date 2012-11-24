package net.simpleframework.common;

import static net.simpleframework.common.I18n.$m;

import java.util.Calendar;
import java.util.Date;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class DateUtils {

	public static final long HOUR_PERIOD = 60 * 60;

	public static final long DAY_PERIOD = HOUR_PERIOD * 24;

	public static long to24Hour() {
		final Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.add(Calendar.DATE, 1);
		return (calendar.getTimeInMillis() - System.currentTimeMillis()) / 1000;
	}

	public static String getDifferenceDate(final Date from) {
		if (from == null) {
			return "";
		}
		return getDifferenceDate(System.currentTimeMillis() - from.getTime());
	}

	public static String getDifferenceDate(long dur) {
		dur = dur / 1000;
		final String s = $m("DateUtils.0");
		final String m = $m("DateUtils.1");
		final String h = $m("DateUtils.2");
		final String d = $m("DateUtils.3");
		final String p = "";
		final StringBuilder sb = new StringBuilder();
		if (dur < 60) {
			sb.append(dur).append(p).append(s);
		} else if (dur < HOUR_PERIOD) {
			final long ii = dur / 60;
			final long jj = dur % 60;
			sb.append(ii).append(p).append(m).append(p).append(jj).append(p).append(s);
		} else if (dur < DAY_PERIOD) {
			final long ii = dur / HOUR_PERIOD;
			final long jj = dur % HOUR_PERIOD / 60;
			sb.append(ii).append(p).append(h).append(p).append(jj).append(p).append(m);
		} else {
			final long ii = dur / DAY_PERIOD;
			final long jj = dur % DAY_PERIOD / HOUR_PERIOD;
			sb.append(ii).append(p).append(d).append(p).append(jj).append(p).append(h);
		}
		return sb.toString();
	}

	public static String getRelativeDate(final Date date) {
		final long tstamp = date.getTime();
		final long t0 = System.currentTimeMillis();
		final long dt = t0 - tstamp;
		final long secs = dt / 1000L;
		long mins = secs / 60L;
		long hours = mins / 60L;
		final long days = hours / 24L;
		final StringBuilder sb = new StringBuilder();
		if (days != 0L) {
			sb.append(days).append($m("DateUtils.3"));
		} else if ((hours -= days * 24L) != 0L) {
			sb.append(hours).append($m("DateUtils.2"));
		} else if ((mins -= (days * 24L + hours) * 60L) != 0L) {
			sb.append(mins).append($m("DateUtils.1"));
		}
		if (days != 0L || hours != 0L || mins != 0L) {
			sb.append($m("DateUtils.4"));
		} else {
			sb.append($m("DateUtils.5"));
		}
		return sb.toString();
	}

	public static Calendar getTimeCalendar(final TimeDistance td) {
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		if (td == TimeDistance.day2) {
			cal.add(Calendar.DATE, -1);
		} else if (td == TimeDistance.week) {
			cal.add(Calendar.DATE, -6);
		} else if (td == TimeDistance.month) {
			cal.add(Calendar.MONTH, -1);
		} else if (td == TimeDistance.month3) {
			cal.add(Calendar.MONTH, -4);
		} else if (td == TimeDistance.year) {
			cal.add(Calendar.YEAR, -1);
		}
		return cal;
	}

	public static Calendar getTimeCalendar(final String time) {
		TimeDistance td;
		try {
			td = TimeDistance.valueOf(time);
		} catch (final Exception e) {
			td = TimeDistance.day;
		}
		return getTimeCalendar(td);
	}

	public static enum TimeDistance {
		/**
		 * 一天内
		 */
		day,

		/**
		 * 两天内
		 */
		day2,

		/**
		 * 一周内
		 */
		week,

		/**
		 * 一月内
		 */
		month,

		/**
		 * 三月内
		 */
		month3,

		/**
		 * 一年内
		 */
		year
	}
}
