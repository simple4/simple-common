package net.simpleframework.common.ado.query;

import java.util.Collection;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IDataQuery<T> {

	/**
	 * 获取下一个可用的对象，null表示已没有可用的数据
	 * 
	 * @return
	 */
	T next();

	/**
	 * 移动游标
	 * 
	 * @param pos
	 */
	void move(int pos);

	/**
	 * 当前游标的位置
	 * 
	 * @return
	 */
	int position();

	int getCount();

	void setCount(int count);

	int getFetchSize();

	/**
	 * 表示一次从数据源获取对象到DataQuery中的数量
	 * 
	 * @param fetchSize
	 */
	void setFetchSize(int fetchSize);

	void reset();

	void close();

	Collection<IDataQueryListener<T>> getListeners();

	void addListener(IDataQueryListener<T> listener);

	boolean removeListener(IDataQueryListener<T> listener);
}
