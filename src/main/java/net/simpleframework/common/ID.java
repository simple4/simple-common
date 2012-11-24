package net.simpleframework.common;

import java.io.Serializable;
import java.util.UUID;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface ID extends Serializable {

	/**
	 * 获取id的值
	 * 
	 * @return
	 */
	Object getValue();

	@SuppressWarnings("serial")
	public abstract class AbstractID<T extends Comparable<T>> implements ID,
			Comparable<AbstractID<T>> {
		protected T id;

		@Override
		public T getValue() {
			return id;
		}

		@Override
		public boolean equals(final Object obj) {
			if (id != null) {
				final Object vObj = obj instanceof ID ? ((ID) obj).getValue() : obj;
				if (id instanceof Number && vObj instanceof Number) {
					return ((Number) id).longValue() == ((Number) vObj).longValue();
				} else {
					return id.equals(vObj);
				}
			} else if (obj == null) {
				return true;
			}
			return super.equals(obj);
		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : super.hashCode();
		}

		@Override
		public String toString() {
			return Convert.toString(getValue());
		}

		@Override
		public int compareTo(final AbstractID<T> o) {
			return id != null && o.id != null ? id.compareTo(o.id) : 0;
		}
	}

	public static class StringID extends AbstractID<String> {
		public StringID(final String id) {
			this.id = id.trim();
		}

		private static final long serialVersionUID = 8283766253505696610L;
	}

	public static class IntegerID extends AbstractID<Integer> {

		public IntegerID(final int id) {
			this.id = id;
		}

		private static final long serialVersionUID = 8864098349861539868L;
	}

	public static class LongID extends AbstractID<Long> {

		public LongID(final long id) {
			this.id = id;
		}

		private static final long serialVersionUID = 4193421687986152568L;
	}

	public static ID nullId = new ID() {
		private static final long serialVersionUID = 3207817821426296535L;

		@Override
		public Object getValue() {
			return null;
		}
	};

	public static Class<?> ID_TYPE = String.class;

	public static abstract class Gen {
		public static ID uuid() {
			final String s = UUID.randomUUID().toString();
			final StringBuilder sb = new StringBuilder(32);
			sb.append(s.substring(0, 8)).append(s.substring(9, 13)).append(s.substring(14, 18))
					.append(s.substring(19, 23)).append(s.substring(24));
			return id(sb.toString());
		}

		static Object lock = new Object();

		static long COUNTER = 0;

		/**
		 * 在同一个虚拟机下产生一个唯一的ID，其格式为[time] - [counter]
		 */
		public static ID uid() {
			final long time = System.currentTimeMillis();
			long id;
			synchronized (lock) {
				id = COUNTER++;
			}
			return id(Long.toString(time, Character.MAX_RADIX)
					+ Long.toString(id, Character.MAX_RADIX));
		}

		public static ID id(final Object id) {
			if (id == null || id instanceof ID) {
				return (ID) id;
			}
			if (id instanceof Long || Long.class.isAssignableFrom(ID_TYPE)) {
				return new LongID(Convert.toLong(id));
			} else if (id instanceof Number || Number.class.isAssignableFrom(ID_TYPE)) {
				return new IntegerID(Convert.toInt(id));
			} else {
				final String s = Convert.toString(id);
				return StringUtils.hasText(s) ? new StringID(s) : nullId;
			}
		}
	}
}
