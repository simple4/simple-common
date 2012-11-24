package net.simpleframework.common.logger;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class Log4jLog extends LogImpl {

	@Override
	public void debug(final String message, final Object... args) {
	}

	@Override
	public void debug(final Throwable e, final String message, final Object... args) {
	}

	@Override
	public void trace(final String message, final Object... args) {
	}

	@Override
	public void trace(final Throwable e, final String message, final Object... args) {
	}

	@Override
	public void info(final String message, final Object... args) {
	}

	@Override
	public void info(final Throwable e, final String message, final Object... args) {
	}

	@Override
	public void warn(final String message, final Object... args) {
	}

	@Override
	public void warn(final Throwable e, final String message, final Object... args) {
	}

	@Override
	public void error(final String message, final Object... args) {
	}

	@Override
	public void error(final Throwable e, final String message, final Object... args) {
	}
}
