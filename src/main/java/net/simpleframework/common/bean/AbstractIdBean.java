package net.simpleframework.common.bean;

import net.simpleframework.common.ID;
import net.simpleframework.common.ObjectEx;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@SuppressWarnings("serial")
public abstract class AbstractIdBean extends ObjectEx implements IIdBeanAware {

	public AbstractIdBean() {
		enableAttributes();
	}

	private ID id;

	@Override
	public ID getId() {
		return id;
	}

	@Override
	public void setId(final ID id) {
		this.id = id;
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof AbstractIdBean) {
			return getId().equals(((IIdBeanAware) obj).getId());
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		final ID id = getId();
		return id != null ? id.hashCode() : super.hashCode();
	}
}
