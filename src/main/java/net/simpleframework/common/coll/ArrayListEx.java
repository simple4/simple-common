package net.simpleframework.common.coll;

import java.util.ArrayList;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings({ "serial", "unchecked" })
public abstract class ArrayListEx<T extends ArrayListEx<T, M>, M> extends ArrayList<M> {

	public T append(final M... btns) {
		if (btns != null) {
			for (final M btn : btns) {
				add(btn);
			}
		}
		return (T) this;
	}
}
