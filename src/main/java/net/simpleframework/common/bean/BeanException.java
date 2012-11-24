package net.simpleframework.common.bean;

import net.simpleframework.common.SimpleRuntimeException;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class BeanException extends SimpleRuntimeException {

	public BeanException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	public static BeanException of(final String msg) {
		return _of(BeanException.class, msg, null);
	}

	public static BeanException of(final Throwable throwable) {
		return _of(BeanException.class, null, throwable);
	}

	private static final long serialVersionUID = 8998574687653782282L;
}
