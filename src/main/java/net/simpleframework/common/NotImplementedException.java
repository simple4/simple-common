package net.simpleframework.common;

import static net.simpleframework.common.I18n.$m;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class NotImplementedException extends SimpleRuntimeException {

	public NotImplementedException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	public static NotImplementedException of(final Class<?> objectClass, final String method) {
		return _of(NotImplementedException.class,
				$m("NotImplementedException.0", objectClass.getName(), method), null);
	}

	private static final long serialVersionUID = 6384740594048001510L;
}
