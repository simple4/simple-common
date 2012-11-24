package net.simpleframework.common.html.element;

import net.simpleframework.common.Convert;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.ArrayUtils;
import net.simpleframework.common.html.HtmlUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings("unchecked")
public abstract class AbstractButtonEx<T extends AbstractLinkElement<T>> extends
		AbstractLinkElement<T> {
	/* 是否选中 */
	private boolean checked;

	/* 是否分割类型的按钮 */
	private boolean separator;

	/* 按钮的图标 */
	private String iconClass;

	/* 是否显示下拉菜单 */
	private boolean menuIcon;

	public AbstractButtonEx() {
	}

	public AbstractButtonEx(final Object text) {
		setText(Convert.toString(text));
	}

	public boolean isChecked() {
		return checked;
	}

	public T setChecked(final boolean checked) {
		this.checked = checked;
		return (T) this;
	}

	public boolean isSeparator() {
		return separator;
	}

	public T setSeparator(final boolean separator) {
		this.separator = separator;
		return (T) this;
	}

	public String getIconClass() {
		return iconClass;
	}

	public T setIconClass(final String iconClass) {
		this.iconClass = iconClass;
		return (T) this;
	}

	public boolean isMenuIcon() {
		return menuIcon;
	}

	public T setMenuIcon(final boolean menuIcon) {
		this.menuIcon = menuIcon;
		return (T) this;
	}

	@Override
	public String getText() {
		final StringBuilder sb = new StringBuilder();
		final String iconClass = getIconClass();
		if (StringUtils.hasText(iconClass)) {
			sb.append(getIcon(iconClass));
		}
		sb.append(super.getText());
		if (isMenuIcon()) {
			sb.append(getDownMenu());
		}
		return sb.toString();
	}

	protected String getIcon(final String iconClass) {
		return "<span class=\"left_icon " + iconClass + "\"></span>";
	}

	protected String getDownMenu() {
		return "<span class=\"right_down_menu\"></span>";
	}

	private static String SEPARATOR_HTML = "<span style=\"display: inline-block; width: 6px;\"></span>";

	@Override
	public String toString() {
		if (isSeparator()) {
			return SEPARATOR_HTML;
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		final String title = getTitle();
		if (StringUtils.hasText(title)) {
			sb.append("<div style=\"display: none;\">");
			sb.append(HtmlUtils.convertHtmlLines(title)).append("</div>");
		}
		return sb.toString();
	}

	@Override
	protected String className(final String... classNames) {
		String[] nClassNames = classNames;
		if (isDisabled()) {
			nClassNames = ArrayUtils.add(new String[] { "disabled_color" }, classNames);
		}
		if (isChecked()) {
			nClassNames = ArrayUtils.add(new String[] { "simple_btn_checked" }, classNames);
		}
		return super.className(nClassNames);
	}
}
