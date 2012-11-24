package net.simpleframework.common.html.element;

import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class Meta extends NamedObject<Meta> {
	private String httpEquiv;

	private String content;

	public Meta(final String httpEquiv, final String content) {
		this.httpEquiv = httpEquiv;
		this.content = content;
	}

	public String getHttpEquiv() {
		return httpEquiv;
	}

	public Meta setHttpEquiv(final String httpEquiv) {
		this.httpEquiv = httpEquiv;
		return this;
	}

	public String getContent() {
		return content;
	}

	public Meta setContent(final String content) {
		this.content = content;
		return this;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("<meta");
		final String name = getName();
		if (StringUtils.hasText(name)) {
			sb.append(" name=\"").append(name).append("\"");
		} else {
			final String httpEquiv = getHttpEquiv();
			if (StringUtils.hasText(httpEquiv)) {
				sb.append(" http-equiv=\"").append(httpEquiv).append("\"");
			}
		}
		final String content = getContent();
		if (StringUtils.hasText(content)) {
			sb.append(" content=\"").append(content).append("\"");
		}
		sb.append(" />");
		return sb.toString();
	}
}
