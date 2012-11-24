package net.simpleframework.common;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ObjectUtils {

	public static boolean objectEquals(final Object newVal, final Object oldVal) {
		if (newVal == oldVal) {
			return true;
		} else if ((newVal == null) || (oldVal == null)) {
			return false;
		} else if (newVal.getClass().isArray() && oldVal.getClass().isArray()) {
			final int nLength = Array.getLength(newVal);
			final int oLength = Array.getLength(oldVal);
			if (nLength != oLength) {
				return false;
			}
			for (int i = 0; i < nLength; i++) {
				if (!objectEquals(Array.get(newVal, i), Array.get(oldVal, i))) {
					return false;
				}
			}
			return true;
		} else {
			return newVal.equals(oldVal);
		}
	}

	public static int length(final Object obj) {
		if (obj == null) {
			return 0;
		}
		if (obj instanceof String) {
			return ((String) obj).length();
		}
		if (obj instanceof Collection) {
			return ((Collection<?>) obj).size();
		}
		if (obj instanceof Map) {
			return ((Map<?, ?>) obj).size();
		}
		int count;
		if (obj instanceof Iterator) {
			final Iterator<?> iter = (Iterator<?>) obj;
			count = 0;
			while (iter.hasNext()) {
				count++;
				iter.next();
			}
			return count;
		}
		if (obj instanceof Enumeration) {
			final Enumeration<?> enumeration = (Enumeration<?>) obj;
			count = 0;
			while (enumeration.hasMoreElements()) {
				count++;
				enumeration.nextElement();
			}
			return count;
		}
		if (obj.getClass().isArray() == true) {
			return Array.getLength(obj);
		}
		return -1;
	}
}
