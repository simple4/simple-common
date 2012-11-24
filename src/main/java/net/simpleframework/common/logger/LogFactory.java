package net.simpleframework.common.logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class LogFactory {

	static Map<Class<?>, Log> lCache;
	static {
		lCache = new ConcurrentHashMap<Class<?>, Log>();
	}

	private static Log createLog(final Class<?> beanClass) {
		return new JdkLog(beanClass.getName());
	}

	public static Log getLogger(final Class<?> beanClass) {
		Log log = lCache.get(beanClass);
		if (log == null) {
			lCache.put(beanClass, log = createLog(beanClass));
		}
		return log;
	}
}
