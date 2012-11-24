package net.simpleframework.common.ado;

import java.io.Serializable;
import java.util.Date;

import net.simpleframework.common.Convert;
import net.simpleframework.common.ObjectUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class FilterItem implements Serializable {
	private EFilterRelation relation;

	private Object value;

	private EFilterOpe ope;

	private String ovalue;

	public FilterItem(final EFilterRelation relation, final Object value) {
		this(relation, value, null);
	}

	public FilterItem(final EFilterRelation relation, final Object value, final EFilterOpe ope) {
		this.relation = relation;
		this.value = value;
		this.ope = ope;
	}

	public EFilterRelation getRelation() {
		return relation;
	}

	public void setRelation(final EFilterRelation relation) {
		this.relation = relation;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(final Object value) {
		this.value = value;
	}

	public EFilterOpe getOpe() {
		return ope;
	}

	public void setOpe(final EFilterOpe ope) {
		this.ope = ope;
	}

	public String getOvalue() {
		return ovalue;
	}

	public void setOvalue(final String ovalue) {
		this.ovalue = ovalue;
	}

	public boolean isDelete(final Object v, final Class<?> propertyType) {
		final EFilterRelation r = getRelation();
		final Object v2 = getValue();
		if (r == EFilterRelation.equal && ObjectUtils.objectEquals(v, v2)) {
			return false;
		} else if (r == EFilterRelation.not_equal && !ObjectUtils.objectEquals(v, v2)) {
			return false;
		} else if (r == EFilterRelation.like && v != null && v2 != null
				&& Convert.toString(v).contains(Convert.toString(v2))) {
			return false;
		} else if (Number.class.isAssignableFrom(propertyType)) {
			final double d = Convert.toDouble(v, Double.MIN_VALUE);
			final double d2 = Convert.toDouble(v2, Double.MIN_VALUE);
			if ((r == EFilterRelation.gt && d > d2) || (r == EFilterRelation.gt_equal && d >= d2)
					|| (r == EFilterRelation.lt && d < d2) || (r == EFilterRelation.lt_equal && d <= d2)) {
				return false;
			}
		} else if (Date.class.isAssignableFrom(propertyType)) {
			final Date d = (Date) v;
			final Date d2 = (Date) v2;
			if (d != null && d2 != null) {
				if ((r == EFilterRelation.gt && d.after(d2))
						|| (r == EFilterRelation.gt_equal && (d.after(d2) || d.equals(d2)))
						|| (r == EFilterRelation.lt && d.before(d2))
						|| (r == EFilterRelation.lt_equal && (d.before(d2) || d.equals(d2)))) {
					return false;
				}
			}
		}
		return true;
	}

	private static final long serialVersionUID = 598446923126901786L;
}
