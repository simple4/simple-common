package net.simpleframework.common.html.element;

import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings("unchecked")
public abstract class AbstractInputElement<T extends AbstractInputElement<T>> extends
		AbstractElement<T> {
	private boolean readonly;

	private EInputType inputType;

	/* 是否选中，checkbox和radio */
	private boolean checked;

	/* 仅对textarea */
	private int rows = 4;

	public boolean isReadonly() {
		return readonly;
	}

	public T setReadonly(final boolean readonly) {
		this.readonly = readonly;
		return (T) this;
	}

	public EInputType getInputType() {
		return inputType == null ? EInputType.text : inputType;
	}

	public T setInputType(final EInputType inputType) {
		this.inputType = inputType;
		return (T) this;
	}

	public boolean isChecked() {
		return checked;
	}

	public T setChecked(final boolean checked) {
		this.checked = checked;
		return (T) this;
	}

	public int getRows() {
		return rows;
	}

	public T setRows(final int rows) {
		this.rows = rows;
		return (T) this;
	}

	public T addSelectOptions(final Enum<?>... vals) {
		final StringBuilder sb = new StringBuilder();
		int i = 0;
		for (final Enum<?> v : vals) {
			if (i++ > 0) {
				sb.append(";");
			}
			sb.append(v.ordinal()).append("=").append(v);
		}
		return setText(sb.toString());
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		final EInputType type = getInputType();
		if (type == EInputType.select || type == EInputType.textarea) {
			sb.append("<").append(type);
		} else {
			sb.append("<input");
			if (type == EInputType.textButton || type == EInputType.multiSelect) {
				sb.append(" type='text'");
			} else {
				sb.append(" type='").append(type).append("'");
			}
		}
		doAttri(sb);
		if (type == EInputType.select || type == EInputType.textarea) {
			sb.append(">");
			if (type == EInputType.select) {
				final String[] opts = StringUtils.split(getText(), ";");
				if (opts != null) {
					for (final String opt : opts) {
						sb.append("<option");
						final String[] optArr = StringUtils.split(opt, "=");
						final boolean b = optArr.length > 1;
						if (b) {
							sb.append(" value='").append(optArr[0]).append("'");
						}
						sb.append(">").append(optArr[b ? 1 : 0]).append("</option>");
					}
				}
			} else {
				sb.append(StringUtils.blank(getText()));
			}
			sb.append("</").append(type).append(">");
		} else {
			final String txt = getText();
			if (StringUtils.hasText(txt)) {
				sb.append(" value=\"").append(txt).append("\"");
			} else {
				if (type == EInputType.checkbox || type == EInputType.radio) {
					sb.append(" value=\"true\"");
				}
			}
			sb.append(" />");
		}
		return sb.toString();
	}

	@Override
	protected void doAttri(final StringBuilder sb) {
		super.doAttri(sb);
		final EInputType type = getInputType();
		if (isReadonly()) {
			sb.append(" readonly");
		}
		if (type == EInputType.checkbox || type == EInputType.radio) {
			if (isChecked()) {
				sb.append(" checked");
			}
		}
		if (type == EInputType.textarea) {
			attri(sb, "rows");
		}
	}
}
