package net.simpleframework.common.html.element;

import static net.simpleframework.common.I18n.$m;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import net.simpleframework.common.StringUtils;
import net.simpleframework.common.bean.BeanException;
import net.simpleframework.common.bean.BeanUtils;
import net.simpleframework.common.coll.ArrayListEx;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.html.HtmlEncoder;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings("unchecked")
public abstract class AbstractElement<T extends AbstractElement<T>> extends TextObject<T> {
	private String id;

	private String className;

	private String style;

	private boolean disabled;

	private String onclick, ondblclick;

	private String title;

	private Map<String, Object> attributes;

	public String getId() {
		return id;
	}

	public T setId(final String id) {
		this.id = id;
		return (T) this;
	}

	public String getStyle() {
		return style;
	}

	public T setStyle(final String style) {
		this.style = style;
		return (T) this;
	}

	public T addStyle(final String style) {
		final Set<String> set = toSet(getStyle());
		set.addAll(toSet(style));
		this.style = StringUtils.join(set, ";");
		return (T) this;
	}

	public String getOnclick() {
		return onclick;
	}

	public T setOnclick(final String onclick) {
		this.onclick = onclick;
		return (T) this;
	}

	public String getOndblclick() {
		return ondblclick;
	}

	public T setOndblclick(final String ondblclick) {
		this.ondblclick = ondblclick;
		return (T) this;
	}

	public String getClassName() {
		return className;
	}

	public T setClassName(final String className) {
		this.className = className;
		return (T) this;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public T setDisabled(final boolean disabled) {
		this.disabled = disabled;
		return (T) this;
	}

	public String getTitle() {
		return title;
	}

	public T setTitle(final String title) {
		this.title = title;
		return (T) this;
	}

	public T addElements(final AbstractElement<?>... elements) {
		final StringBuilder sb = new StringBuilder();
		if (elements != null) {
			for (final AbstractElement<?> element : elements) {
				sb.append(element);
			}
			setText(sb.toString());
		}
		return (T) this;
	}

	public T addElements(final ArrayListEx<?, ?> elements) {
		if (elements != null) {
			setText(elements.toString());
		}
		return (T) this;
	}

	public T addAttribute(final String key, final Object val) {
		if (BeanUtils.hasProperty(this, key)) {
			throw BeanException.of($m("AbstractElement.0", key));
		}
		if (attributes == null) {
			attributes = new KVMap();
		}
		attributes.put(key, val);
		return (T) this;
	}

	@Override
	protected void doAttri(final StringBuilder sb) {
		attri(sb, "id").attri(sb, "name").attri(sb, "style").attri(sb, "className");
		if (!isDisabled()) {
			attri(sb, "onclick");
			attri(sb, "ondblclick");
		}
		if (attributes != null) {
			for (final Map.Entry<String, Object> e : attributes.entrySet()) {
				sb.append(" ").append(e.getKey()).append("=\"");
				sb.append(HtmlEncoder.text(String.valueOf(e.getValue()))).append("\"");
			}
		}
	}

	@Override
	protected T attri(final StringBuilder sb, final String attri) {
		if ("className".equals(attri)) {
			final String className = className();
			if (StringUtils.hasText(className)) {
				sb.append(" class=\"").append(className).append("\"");
			}
		} else {
			super.attri(sb, attri);
		}
		return (T) this;
	}

	protected String className(final String... classNames) {
		return StringUtils.join(ArrayUtils.add(classNames, getClassName()), " ");
	}

	public static Set<String> toSet(final String style) {
		final Set<String> set = new LinkedHashSet<String>();
		if (StringUtils.hasText(style)) {
			set.addAll(Arrays.asList(StringUtils.split(style.replace(" ", "").toLowerCase(), ";")));
		}
		return set;
	}
}
