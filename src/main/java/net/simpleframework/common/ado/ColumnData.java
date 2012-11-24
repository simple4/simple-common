package net.simpleframework.common.ado;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import net.simpleframework.common.ObjectEx;
import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ColumnData extends ObjectEx implements Serializable {

	private String columnName;

	private String columnText;

	private Class<?> propertyClass;

	private boolean visible = true;

	private EOrder order;

	public Collection<FilterItem> filterItems;

	public ColumnData(final String columnName) {
		this(columnName, null);
	}

	public ColumnData(final String columnName, final String columnText) {
		this(columnName, columnText, null);
	}

	public ColumnData(final String columnName, final String columnText, final Class<?> propertyClass) {
		this.columnName = columnName;
		this.columnText = columnText;
		this.propertyClass = propertyClass;
		enableAttributes();
	}

	public String getColumnName() {
		return columnName;
	}

	public ColumnData setColumnName(final String columnName) {
		this.columnName = columnName;
		return this;
	}

	public String getColumnText() {
		return StringUtils.text(columnText, getColumnName());
	}

	public ColumnData setColumnText(final String columnText) {
		this.columnText = columnText;
		return this;
	}

	public Class<?> getPropertyClass() {
		return propertyClass;
	}

	public ColumnData setPropertyClass(final Class<?> propertyClass) {
		this.propertyClass = propertyClass;
		return this;
	}

	public boolean isVisible() {
		return visible;
	}

	public ColumnData setVisible(final boolean visible) {
		this.visible = visible;
		return this;
	}

	public EOrder getOrder() {
		return order == null ? EOrder.normal : order;
	}

	public ColumnData setOrder(final EOrder order) {
		this.order = order;
		return this;
	}

	public Collection<FilterItem> getFilterItems() {
		if (filterItems == null) {
			filterItems = new ArrayList<FilterItem>();
		}
		return filterItems;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj == this) {
			return true;
		} else if (obj instanceof ColumnData) {
			return toString().equals(((ColumnData) obj).toString());
		} else {
			return super.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	private static final long serialVersionUID = -2338071977267680196L;
}
