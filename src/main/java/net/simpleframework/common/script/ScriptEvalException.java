package net.simpleframework.common.script;

import net.simpleframework.common.SimpleRuntimeException;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ScriptEvalException extends SimpleRuntimeException {
	public ScriptEvalException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	public static ScriptEvalException of(final Throwable throwable) {
		return _of(ScriptEvalException.class, null, throwable);
	}

	private static final long serialVersionUID = -4496578242304451585L;
}