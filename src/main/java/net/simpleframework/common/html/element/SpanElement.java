package net.simpleframework.common.html.element;

import net.simpleframework.common.Convert;
import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class SpanElement extends AbstractElement<SpanElement> {
	public static SpanElement SEP = new SpanElement("|").setStyle("margin: 0px 4px;");

	public static SpanElement NAV = new SpanElement("&raquo;").setStyle("margin: 0px 2px;");

	public static SpanElement ELLIPSIS = new SpanElement("&hellip;").setStyle("color: black;");

	public SpanElement(final Object text) {
		setText(Convert.toString(text));
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("<span");
		doAttri(sb);
		sb.append(">");
		final String text = getText();
		if (StringUtils.hasText(text)) {
			sb.append(text);
		}
		sb.append("</span>");
		return sb.toString();
	}
}
