package net.simpleframework.common.html.element;

import net.simpleframework.common.coll.ArrayListEx;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ElementList extends ArrayListEx<ElementList, AbstractElement<?>> {

	public static ElementList of(final AbstractElement<?>... items) {
		return new ElementList().append(items);
	}

	private static final long serialVersionUID = -8718459221197045855L;
}
