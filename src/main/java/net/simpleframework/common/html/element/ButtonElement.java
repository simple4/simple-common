package net.simpleframework.common.html.element;

import static net.simpleframework.common.I18n.$m;
import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ButtonElement extends AbstractElement<ButtonElement> {

	public static final ButtonElement WINDOW_CLOSE = new ButtonElement().setOnclick(
			"$win(this).close();").setText($m("Button.Close"));

	public static final ButtonElement okBtn() {
		return new ButtonElement().setClassName("button2").setText($m("Button.Ok"));
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("<input type=\"button\"");
		doAttri(sb);
		final String txt = getText();
		if (StringUtils.hasText(txt)) {
			sb.append(" value=\"").append(txt).append("\"");
		}
		sb.append(" />");
		return sb.toString();
	}
}
