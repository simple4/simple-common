package net.simpleframework.common.html.element;

import net.simpleframework.common.Convert;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class LinkElement extends AbstractLinkElement<LinkElement> {

	public LinkElement(final Object text) {
		setText(Convert.toString(text));
	}
}
