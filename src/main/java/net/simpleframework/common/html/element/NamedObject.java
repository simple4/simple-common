package net.simpleframework.common.html.element;

import net.simpleframework.common.bean.BeanUtils;
import net.simpleframework.common.html.HtmlEncoder;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings("unchecked")
public abstract class NamedObject<T extends NamedObject<T>> {
	private String name;

	public String getName() {
		return name;
	}

	public T setName(final String name) {
		this.name = name;
		return (T) this;
	}

	@Override
	public boolean equals(final Object obj) {
		if (name != null && obj instanceof TextObject) {
			return name.equals(((TextObject<?>) obj).getName());
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : super.hashCode();
	}

	protected void doAttri(final StringBuilder sb) {
	}

	protected T attri(final StringBuilder sb, final String attri) {
		final Object obj = BeanUtils.getProperty(this, attri);
		if (obj != null) {
			sb.append(" ").append(attri).append("=\"");
			sb.append(HtmlEncoder.text(String.valueOf(obj))).append("\"");
		}
		return (T) this;
	}
}
