package net.simpleframework.common.ado.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ListDataObjectQuery<T> extends AbstractDataQuery<T> {

	private List<T> list;

	public ListDataObjectQuery() {
		this(null);
	}

	public ListDataObjectQuery(final Collection<T> list) {
		this.list = list == null ? new ArrayList<T>() : new ArrayList<T>(list);
	}

	public List<T> getList() {
		return list;
	}

	@Override
	public T next() {
		return ++i < getCount() && list != null ? list.get(i) : null;
	}

	@Override
	public int getCount() {
		if (list != null && count < 0) {
			count = list.size();
		}
		return count;
	}

	@Override
	public int getFetchSize() {
		return 0;
	}

	@Override
	public void setFetchSize(final int fetchSize) {
	}
}
