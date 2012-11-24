package net.simpleframework.common.ado;

import net.simpleframework.common.ado.IParamsValue.AbstractParamsValue;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class UniqueValue extends AbstractParamsValue {
	private static final long serialVersionUID = 3290227392192678851L;

	public UniqueValue(final Object... values) {
		setValues(values);
	}

	@Override
	public String getKey() {
		return valuesToString();
	}
}
