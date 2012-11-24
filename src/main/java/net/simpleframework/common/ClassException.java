package net.simpleframework.common;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ClassException extends SimpleRuntimeException {

	public ClassException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	public static ClassException of(final Throwable throwable) {
		return _of(ClassException.class, null, throwable);
	}

	private static final long serialVersionUID = 6128245380078511011L;
}
