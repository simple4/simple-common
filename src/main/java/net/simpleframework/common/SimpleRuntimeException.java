package net.simpleframework.common;

import net.simpleframework.common.coll.KVMap;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings("serial")
public abstract class SimpleRuntimeException extends RuntimeException {
	private final KVMap attributes = new KVMap();

	public SimpleRuntimeException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	public SimpleRuntimeException putVal(final String key, final Object val) {
		attributes.add(key, val);
		return this;
	}

	public Object getVal(final String key) {
		return attributes.get(key);
	}

	public static <T extends RuntimeException> T _of(final Class<T> exClazz, final String msg) {
		return _of(exClazz, msg, null);
	}

	@SuppressWarnings("unchecked")
	public static <T extends RuntimeException> T _of(final Class<T> exClazz, final String msg,
			Throwable throwable) {
		if (throwable == null) {
			try {
				return exClazz.getConstructor(String.class, Throwable.class).newInstance(msg, null);
			} catch (final Exception e) {
			}
		}
		if (!exClazz.isAssignableFrom(throwable.getClass())) {
			throwable = ThrowableUtils.convertThrowable(throwable);
			try {
				return exClazz.getConstructor(String.class, Throwable.class)
						.newInstance(msg, throwable);
			} catch (final Throwable e) {
			}
		}
		return (T) throwable;
	}
}
