package net.simpleframework.common.web;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public enum EUrlMatch {

	/**
	 * 结束匹配
	 */
	endsWith,

	/**
	 * 开始匹配
	 */
	startsWith,

	/**
	 * 包含匹配
	 */
	contains,

	/**
	 * 等于
	 */
	equals
}
