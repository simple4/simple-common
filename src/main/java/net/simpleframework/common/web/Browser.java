package net.simpleframework.common.web;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class Browser implements Serializable {

	public static Browser get(final HttpServletRequest httpRequest) {
		final HttpSession httpSession = httpRequest.getSession();
		Browser browser = (Browser) httpSession.getAttribute("@browser");
		if (browser == null) {
			httpSession.setAttribute("@browser",
					browser = new Browser(httpRequest.getHeader("User-Agent")));
		}
		return browser;
	}

	private boolean trident = false, webKit = false, gecko = false, presto = false;

	/* 主版本 */
	private int version = 0;

	public Browser(final String userAgent) {
		int p;
		trident = (p = userAgent.indexOf("MSIE")) > -1;
		if (trident) {
			final int p2 = userAgent.indexOf('.', p);
			try {
				version = Integer.parseInt(userAgent.substring(p + 4, p2).trim());
			} catch (final NumberFormatException e) {
			}
		} else {
			webKit = userAgent.contains("AppleWebKit/");
			if (!webKit) {
				gecko = userAgent.contains("Gecko/");
				if (!gecko) {
					presto = userAgent.contains("Presto/");
				}
			}
		}
	}

	public boolean isTrident() {
		return trident;
	}

	public boolean isWebKit() {
		return webKit;
	}

	public boolean isGecko() {
		return gecko;
	}

	public boolean isPresto() {
		return presto;
	}

	public int getVersion() {
		return version;
	}

	private static final long serialVersionUID = 1798765474943067839L;
}
