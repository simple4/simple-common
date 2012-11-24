package net.simpleframework.common.html.element;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings("unchecked")
public abstract class AbstractLinkElement<T extends AbstractElement<T>> extends AbstractElement<T> {
	private String href;

	private String target;

	public String getHref() {
		return href;
	}

	public T setHref(final String href) {
		this.href = href;
		return (T) this;
	}

	public String getTarget() {
		return target;
	}

	public T setTarget(final String target) {
		this.target = target;
		return (T) this;
	}

	@Override
	protected void doAttri(final StringBuilder sb) {
		super.doAttri(sb);
		attri(sb, "target");
		if (!isDisabled()) {
			attri(sb, "href");
		}
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("<a");
		doAttri(sb);
		sb.append(">").append(getText()).append("</a>");
		return sb.toString();
	}
}
