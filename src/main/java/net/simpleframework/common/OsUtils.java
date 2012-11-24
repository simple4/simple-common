package net.simpleframework.common;

import javax.servlet.http.HttpServletRequest;

import net.simpleframework.common.web.HttpUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @title OsUtils.java
 * @package net.simpleframework.common
 * @description TODO(系统信息分析工具)
 * @author shihb(shihaibin.sea@gmail.com, 13466609192)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 * @update 2012-9-3 上午11:38:25
 * @version 4.0
 */

public class OsUtils {
	/**
	 * 
	 * @description TODO(系统枚举)
	 * @version 4.0
	 * @author shihb
	 * @update 2012-9-3 下午12:27:23
	 */
	public static enum OS {
		Windows, Linux, iOS, Android, mango// Windows phone
	}

	/**
	 * 
	 * @description TODO(系统客户端)
	 * @version 4.0
	 * @author shihb
	 * @update 2012-9-3 下午12:27:40
	 */
	public static enum OsClient {

		Pc,

		Mobile,

		Pad
	}

	/**
	 * 
	 * @description TODO(系统信息)
	 * @version 4.0
	 * @author shihb
	 * @update 2012-9-3 下午12:28:24
	 */
	public static class OsInfo {

		public OsInfo() {// 赋予默认值
			os = OS.Windows;
			oc = OsClient.Pc;
			v = Version.getVersion(String.valueOf(6.0f));
		}

		/**
		 * 系统
		 */
		public OS os;
		/**
		 * 系统客户端
		 */
		public OsClient oc;
		/**
		 * 系统版本
		 */
		public Version v;

		@Override
		public String toString() {
			return "{\"OsClient\":\"" + oc.name() + "\",\"OS\":\"" + os.name() + "\",\"version\":"
					+ v.toString() + "}";
		}

	}

	/**
	 * 
	 * @param request
	 * @return
	 * @description TODO(分析访问客户端的系统信息)
	 * @version 4.0
	 * @author shihb
	 * @update 2012-9-3 下午12:28:37
	 */
	public static OsInfo get(final HttpServletRequest request) {
		final String userAgent = HttpUtils.getUserAgent(request);
		final OsInfo osInfo = new OsInfo();
		if (userAgent != null) {
			for (final OsClient osClient : OsClient.values()) {
				if (userAgent.indexOf(osClient.name()) > -1) {
					osInfo.oc = osClient;
				}
			}
			final StringBuilder sb = new StringBuilder();
			for (final OS os : OS.values()) {
				int p = userAgent.indexOf(os.name());
				if (p > -1) {
					osInfo.os = os;
					p = p + os.name().length();
					while (p < userAgent.length()) {
						final char c = userAgent.charAt(p++);
						if (Character.isDigit(c) || c == '.') {
							sb.append(c);
						} else if (c == ';') {
							break;
						}
					}
					osInfo.v = Version.getVersion(sb.length() == 0 ? "0.0.1" : sb.toString());
					sb.delete(0, sb.capacity());
				}
			}
		}
		return osInfo;
	}
}
