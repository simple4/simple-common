package net.simpleframework.common.ado.query;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class DataQueryUtils {

	public static <T> IDataQuery<T> nullQuery() {
		return new ListDataObjectQuery<T>();
	}

	public static <T> Enumeration<T> toEnumeration(final IDataQuery<T> dataQuery) {
		return new Enumeration<T>() {
			private T t;

			@Override
			public boolean hasMoreElements() {
				return (t = dataQuery.next()) != null;
			}

			@Override
			public T nextElement() {
				return t;
			}
		};
	}

	public static <T> List<T> toList(final IDataQuery<T> dataQuery) {
		T t;
		final List<T> al = new ArrayList<T>();
		while ((t = dataQuery.next()) != null) {
			al.add(t);
		}
		return al;
	}
}
