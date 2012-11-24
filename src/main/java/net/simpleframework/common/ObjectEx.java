package net.simpleframework.common;

import java.lang.reflect.Modifier;
import java.util.Enumeration;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.common.logger.Log;
import net.simpleframework.common.logger.LogFactory;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ObjectEx {
	private static final Map<Class<?>, Object> singletonCache;
	static {
		singletonCache = new ConcurrentHashMap<Class<?>, Object>();
	}

	public static <T> T singleton(final Class<T> beanClass, final ISingletonCallback<T> callback) {
		int m;
		if (beanClass == null || Modifier.isInterface(m = beanClass.getModifiers())
				|| Modifier.isAbstract(m)) {
			return null;
		}
		@SuppressWarnings("unchecked")
		T o = (T) singletonCache.get(beanClass);
		if (o == null) {
			singletonCache.put(beanClass, o = ClassUtils.newInstance(beanClass));
			if (callback != null) {
				callback.onCreated(o);
			}
		}
		return o;
	}

	public static <T> T singleton(final Class<T> beanClass) {
		return singleton(beanClass, null);
	}

	public static interface ISingletonCallback<T> {

		/**
		 * 对象创建后触发
		 * 
		 * @param bean
		 */
		void onCreated(T bean);
	}

	public static Object singleton(final String beanClass) throws ClassNotFoundException {
		return singleton(ClassUtils.forName(beanClass));
	}

	private Map<String, Object> namedCache;

	public void enableNamedInstance() {
		namedCache = new ConcurrentHashMap<String, Object>();
	}

	public <T> T getNamedInstance(final String name, final Class<T> objectClass) {
		if (name == null) {
			return null;
		}
		@SuppressWarnings("unchecked")
		T o = (T) namedCache.get(name);
		if (o == null) {
			namedCache.put(name, o = ClassUtils.newInstance(objectClass));
		}
		return o;
	}

	/**
	 * 扩充属性
	 */
	private Map<String, Object> attributes;

	public void enableAttributes() {
		attributes = new ConcurrentHashMap<String, Object>();
	}

	public Object getAttr(final String key) {
		return attributes != null ? attributes.get(key) : null;
	}

	public ObjectEx setAttr(final String key, final Object value) {
		if (attributes == null) {
			throw NotImplementedException.of(getClass(), "enableAttributes");
		}
		if (value == null) {
			removeAttr(key);
		} else {
			attributes.put(key, value);
		}
		return this;
	}

	public Object removeAttr(final String key) {
		return attributes != null ? attributes.remove(key) : null;
	}

	public void clearAttribute() {
		if (attributes != null) {
			attributes.clear();
		}
	}

	public Enumeration<String> getAttrNames() {
		return attributes != null ? new Vector<String>(attributes.keySet()).elements() : null;
	}

	protected final Log log = LogFactory.getLogger(getClass());
}
