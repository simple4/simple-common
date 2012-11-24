package net.simpleframework.common.logger;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class LogImpl implements Log {

	protected String format(final String message, final Object... args) {
		return args.length > 0 ? String.format(message, args) : message;
	}

	@Override
	public void debug(final Throwable e) {
		debug(e, null);
	}

	@Override
	public void trace(final Throwable e) {
		trace(e, null);
	}

	@Override
	public void info(final Throwable e) {
		info(e, null);
	}

	@Override
	public void warn(final Throwable e) {
		warn(e, null);
	}

	@Override
	public void error(final Throwable e) {
		error(e, null);
	}
}
