package net.simpleframework.common.html.element;

import net.simpleframework.common.I18n;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings("unchecked")
public abstract class TextObject<T extends TextObject<T>> extends NamedObject<T> {

	private String text;

	public String getText() {
		return text;
	}

	public T setText(final String text) {
		this.text = I18n.replaceI18n(text);
		return (T) this;
	}

	@Override
	public String toString() {
		return getText();
	}
}
