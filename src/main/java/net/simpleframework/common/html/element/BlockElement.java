package net.simpleframework.common.html.element;

import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class BlockElement extends AbstractElement<BlockElement> {
	public BlockElement() {
	}

	public BlockElement(final String id) {
		setId(id);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("<div");
		doAttri(sb);
		sb.append(">");
		final String text = getText();
		if (StringUtils.hasText(text)) {
			sb.append(text);
		}
		sb.append("</div>");
		return sb.toString();
	}
}
