package net.simpleframework.common.xml;

import net.simpleframework.common.I18n;
import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ItemUIBean extends AbstractElementBean {
	private String id;

	private String text;

	// event
	private String jsClickCallback, jsDblclickCallback;

	public ItemUIBean(final XmlElement xmlElement) {
		super(xmlElement);
	}

	public String getId() {
		if (!StringUtils.hasText(id)) {
			id = StringUtils.hash(getText());
		}
		return id;
	}

	public ItemUIBean setId(final String id) {
		this.id = id;
		return this;
	}

	public String getText() {
		return I18n.replaceI18n(text);
	}

	public ItemUIBean setText(final String text) {
		this.text = text;
		return this;
	}

	public String getJsClickCallback() {
		return jsClickCallback;
	}

	public ItemUIBean setJsClickCallback(final String jsClickCallback) {
		this.jsClickCallback = jsClickCallback;
		return this;
	}

	public String getJsDblclickCallback() {
		return jsDblclickCallback;
	}

	public ItemUIBean setJsDblclickCallback(final String jsDblclickCallback) {
		this.jsDblclickCallback = jsDblclickCallback;
		return this;
	}

	@Override
	protected String[] elementAttributes() {
		return new String[] { "jsClickCallback", "jsDblclickCallback" };
	}
}
