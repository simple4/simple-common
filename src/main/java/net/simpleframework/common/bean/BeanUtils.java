package net.simpleframework.common.bean;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;
import net.simpleframework.common.Version;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.logger.Log;
import net.simpleframework.common.logger.LogFactory;
import net.simpleframework.lib.net.minidev.asm.ASMUtil;
import net.simpleframework.lib.net.minidev.asm.Accessor;
import net.simpleframework.lib.net.minidev.asm.BeansAccess;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class BeanUtils {

	static Log log = LogFactory.getLogger(BeanUtils.class);

	public static boolean hasProperty(final Object bean, final String name) {
		if (bean instanceof Map) {
			return ((Map) bean).containsKey(name);
		} else {
			return getBeansAccess(bean.getClass()).getMap().containsKey(name);
		}
	}

	/**
	 * 获取bean的属性值。取消了反射，采用字节码操作
	 * 
	 * @param bean
	 * @param name
	 * @return
	 */
	public static Object getProperty(final Object bean, final String name) {
		if (bean instanceof Map) {
			return ((Map) bean).get(name);
		} else {
			return getBeansAccess(bean.getClass()).get(bean, name);
		}
	}

	public static void setProperty(final Object bean, final String name, Object value) {
		if (bean instanceof Map) {
			((Map) bean).put(name, value);
		} else {
			final BeansAccess<Object> ba = getBeansAccess(bean.getClass());
			final Accessor accessor = ba.getMap().get(name);
			Method setter;
			if (accessor != null && (setter = accessor.getSetter()) != null) {
				final Class<?> parameterType = setter.getParameterTypes()[0];
				if (Enum.class.isAssignableFrom(parameterType)) {
					if (value instanceof Enum) {
						value = ((Enum) value).name();
					} else if (value instanceof Number) {
						final int i = ((Number) value).intValue();
						value = ((Class<? extends Enum>) parameterType).getEnumConstants()[i].name();
					}
				} else if (ID.class.isAssignableFrom(parameterType)) {
					value = ID.Gen.id(value);
				} else if (Version.class.isAssignableFrom(parameterType)) {
					value = Version.getVersion(String.valueOf(value));
				} else {
					value = Convert.convert(value, parameterType);
				}
				ba.set(bean, name, value);
			} else {
				try {
					bean.getClass()
							.getMethod(ASMUtil.getSetterName(name),
									value == null ? String.class : value.getClass()).invoke(bean, value);
				} catch (final Exception e) {
				}
			}
		}
	}

	public static BeansAccess<Object> getBeansAccess(final Class<?> beanClass) {
		return (BeansAccess<Object>) BeansAccess.get(beanClass);
	}

	public static Set<String> fields(final Class<?> beanClass) {
		return getBeansAccess(beanClass).getMap().keySet();
	}

	public static Map<String, Object> toMap(final Object bean) {
		return toMap(bean, true);
	}

	public static Map<String, Object> toMap(final Object bean, final boolean caseInsensitive) {
		final KVMap map = new KVMap().setCaseInsensitive(caseInsensitive);
		if (bean instanceof Map) {
			for (final Object o : ((Map<?, ?>) bean).entrySet()) {
				final Map.Entry e = (Map.Entry) o;
				map.add(Convert.toString(e.getKey()), e.getValue());
			}
		} else if (bean != null) {
			final BeansAccess<Object> ba = getBeansAccess(bean.getClass());
			for (final Accessor accessor : ba.getMap().values()) {
				try {
					map.add(accessor.getName(), ba.get(bean, accessor.getIndex()));
				} catch (final Throwable e) {
				}
			}
		}
		return map;
	}
}
