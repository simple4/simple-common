package net.simpleframework.common.ado.query;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IDataQueryListener<T> {

	/**
	 * 
	 * @param dataQuery
	 * @param bean
	 * @param pIndex
	 * @param pageEnd
	 */
	void next(IDataQuery<T> dataQuery, T bean, int pIndex, boolean pageEnd);
}
