package net.simpleframework.common.task;

import net.simpleframework.common.ObjectEx;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ExecutorRunnable extends ObjectEx implements Runnable {

	protected abstract void task() throws Exception;

	protected String getTaskname() {
		return getClass().getName();
	}

	@Override
	public void run() {
		try {
			task();
		} catch (final Exception ex) {
			log.warn(ex);
		}
	}
}
