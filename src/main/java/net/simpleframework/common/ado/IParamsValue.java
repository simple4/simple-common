package net.simpleframework.common.ado;

import java.io.Serializable;
import java.util.Date;

import net.simpleframework.common.ID;
import net.simpleframework.common.ObjectEx;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.ArrayUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IParamsValue extends Serializable {

	Object[] getValues();

	String getKey();

	@SuppressWarnings("serial")
	public abstract class AbstractParamsValue extends ObjectEx implements IParamsValue {
		private Object[] values;

		{
			enableAttributes();
		}

		@Override
		public Object[] getValues() {
			return values;
		}

		public void setValues(final Object[] values) {
			this.values = values;
		}

		public void addValues(final Object[] values) {
			if (this.values == null) {
				this.values = values;
			} else {
				this.values = ArrayUtils.add(this.values, values);
			}
		}

		public void addValue(final Object value) {
			addValues(new Object[] { value });
		}

		protected String valuesToString() {
			final StringBuffer sb = new StringBuffer();
			final Object[] values = getValues();
			if (values != null) {
				int i = 0;
				for (final Object v : values) {
					if (i++ > 0) {
						sb.append("-");
					}
					sb.append(valueToString(v));
				}
			}
			return sb.toString();
		}

		public static String valueToString(final Object v) {
			if (v == null) {
				return null;
			}
			if (v.getClass().isPrimitive() || v instanceof Number || v instanceof String
					|| v instanceof Boolean) {
				return String.valueOf(v);
			} else if (v instanceof Date) {
				return String.valueOf(((Date) v).getTime());
			} else if (v instanceof ID) {
				return valueToString(((ID) v).getValue());
			} else {
				return StringUtils.hash(v);
			}
		}
	}
}
