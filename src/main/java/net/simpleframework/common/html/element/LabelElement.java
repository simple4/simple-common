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
public class LabelElement extends AbstractElement<LabelElement> {

	private String forId;

	public LabelElement(final Object text) {
		setText(Convert.toString(text));
	}

	public String getForId() {
		return forId;
	}

	public LabelElement setForId(final String forId) {
		this.forId = forId;
		return this;
	}

	public LabelElement setFor(final AbstractInputElement<?> element) {
		String id = element.getId();
		if (!StringUtils.hasText(id)) {
			id = "element_" + StringUtils.hash(this);
			element.setId(id);
		}
		return setForId(id);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("<label");
		doAttri(sb);
		String forId;
		if (StringUtils.hasText(forId = getForId())) {
			sb.append(" for=\"").append(forId).append("\"");
		}
		sb.append(">");
		final String text = getText();
		if (StringUtils.hasText(text)) {
			sb.append(text);
		}
		sb.append("</label>");
		return sb.toString();
	}
}
