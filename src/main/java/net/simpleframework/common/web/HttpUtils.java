package net.simpleframework.common.web;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.simpleframework.common.Convert;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.TextUtils;
import net.simpleframework.common.coll.ParameterMap;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class HttpUtils {
	public static final String HTML_BLANK_STRING = "&nbsp;";

	public static final String FORWARD_REQUEST_URI_ATTRIBUTE = "javax.servlet.forward.request_uri";

	public static final String FORWARD_QUERY_STRING_ATTRIBUTE = "javax.servlet.forward.query_string";

	public static String stripContextPath(final HttpServletRequest httpRequest, final String url) {
		if (url == null) {
			return null;
		}
		final String cp = httpRequest.getContextPath();
		return url.startsWith(cp) ? url.substring(cp.length()) : url;
	}

	public static String stripJSessionId(final String url) {
		final StringBuilder u = new StringBuilder(url);
		int sessionStart;
		while ((sessionStart = u.toString().indexOf(";jsessionid=")) != -1) {
			int sessionEnd = u.toString().indexOf(';', sessionStart + 1);
			if (sessionEnd == -1) {
				sessionEnd = u.toString().indexOf('?', sessionStart + 1);
			}
			if (sessionEnd == -1) {
				sessionEnd = u.length();
			}
			u.delete(sessionStart, sessionEnd);
		}
		return u.toString();
	}

	public static String getRequestURI(final HttpServletRequest httpRequest) {
		return getRequestURI(httpRequest, true);
	}

	public static String getRequestURI(final HttpServletRequest httpRequest,
			final boolean forwardRequestUri) {
		Object requestURI;
		if (forwardRequestUri
				&& (requestURI = httpRequest.getAttribute(FORWARD_REQUEST_URI_ATTRIBUTE)) != null) {
			return String.valueOf(requestURI);
		} else {
			return httpRequest.getRequestURI();
		}
	}

	public static String getQueryString(final HttpServletRequest httpRequest,
			final boolean forwardRequestUri) {
		if (forwardRequestUri && httpRequest.getAttribute(FORWARD_REQUEST_URI_ATTRIBUTE) != null) {
			return (String) httpRequest.getAttribute(FORWARD_QUERY_STRING_ATTRIBUTE);
		} else {
			return httpRequest.getQueryString();
		}
	}

	public static String getRequestAndQueryStringUrl(final HttpServletRequest httpRequest) {
		return getRequestAndQueryStringUrl(httpRequest, true);
	}

	public static String getRequestAndQueryStringUrl(final HttpServletRequest httpRequest,
			final boolean forwardRequestUri) {
		final String url = getRequestURI(httpRequest, forwardRequestUri);
		final String query = getQueryString(httpRequest, forwardRequestUri);
		return StringUtils.hasText(query) ? url + "?" + query : url;
	}

	public static String getRemoteAddr(final HttpServletRequest httpRequest) {
		String ip = httpRequest.getHeader("x-forwarded-for");
		if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = httpRequest.getHeader("Proxy-Client-IP");
		}
		if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = httpRequest.getHeader("WL-Proxy-Client-IP");
		}
		if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
			ip = httpRequest.getRemoteAddr();
		}
		final String[] arr = StringUtils.split(ip, ",");
		return arr[arr.length - 1].trim();
	}

	public static void setNoCache(final HttpServletResponse httpResponse) {
		httpResponse.setHeader("Cache-Control",
				"max-age=0, must-revalidate, no-cache, no-store, private, post-check=0, pre-check=0");
		httpResponse.setHeader("Pragma", "no-cache");
		httpResponse.setDateHeader("Expires", 0);
	}

	public static final String VALID_SCHEME_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789+.-";

	public static boolean isAbsoluteUrl(final String url) {
		if (url == null) {
			return false;
		}
		int colonPos;
		if ((colonPos = url.indexOf(':')) == -1) {
			return false;
		}
		for (int i = 0; i < colonPos; i++) {
			if (VALID_SCHEME_CHARS.indexOf(url.charAt(i)) == -1) {
				return false;
			}
		}
		return true;
	}

	public static final String wrapContextPath(final HttpServletRequest httpRequest, final String url) {
		final String contextPath = StringUtils.text(httpRequest.getContextPath(), "/");
		if (!StringUtils.hasText(url)) {
			return contextPath;
		}
		if (HttpUtils.isAbsoluteUrl(url)) {
			return url;
		} else if (url.charAt(0) != '/') {
			return url;
		} else if (url.toLowerCase().startsWith(contextPath.toLowerCase())) {
			return url;
		} else {
			return contextPath + url;
		}
	}

	public static boolean loc(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse, final String url) throws IOException {
		if (!httpResponse.isCommitted()) {
			httpResponse.sendRedirect(wrapContextPath(httpRequest, url));
			return true;
		}
		return false;
	}

	/****************************** UserAgent *****************************/

	public static String getUserAgent(final HttpServletRequest request) {
		return StringUtils.blank(request.getHeader("User-Agent"));
	}

	// public static float getIEVersion(final HttpServletRequest httpRequest) {
	// final HttpSession httpSession = httpRequest.getSession();
	// Float ver = (Float) httpSession.getAttribute("IEVersion");
	// if (ver == null) {
	// final String userAgent = getUserAgent(httpRequest);
	// final int p = userAgent.indexOf("MSIE");
	// final int p2 = userAgent.indexOf(';', p);
	// if (p > -1 && p2 > p) {
	// httpSession.setAttribute("IEVersion",
	// ver = Float.parseFloat(userAgent.substring(p + 4, p2)));
	// }
	// }
	// return ver == null ? -1f : ver;
	// }

	/****************************** cookie *****************************/

	public static void addCookie(final HttpServletResponse httpResponse, final String key,
			final Object value) {
		addCookie(httpResponse, key, value, false, "/", -1);
	}

	public static void addCookie(final HttpServletResponse httpResponse, final String key,
			final Object value, final int age) {
		addCookie(httpResponse, key, value, false, "/", age);
	}

	public static void addCookie(final HttpServletResponse httpResponse, final String key,
			final Object value, final boolean secure, final String path, final int age) {
		final String sValue = Convert.toString(value);
		Cookie cookie;
		if (StringUtils.hasText(sValue)) {
			cookie = new Cookie(key, sValue);
			cookie.setMaxAge(age);
		} else {
			cookie = new Cookie(key, "");
			cookie.setMaxAge(0);
		}
		cookie.setSecure(secure);
		cookie.setPath(path);
		httpResponse.addCookie(cookie);
	}

	public static String getCookie(final HttpServletRequest httpRequest, final String key) {
		if (key == null) {
			return null;
		}
		final Cookie[] cookies = httpRequest.getCookies();
		if (cookies != null) {
			for (final Cookie cookie : cookies) {
				if (key.equals(cookie.getName())) {
					return cookie.getValue();
				}
			}
		}
		return null;
	}

	public static OutputStream getBinaryOutputStream(final HttpServletRequest httpRequest,
			final HttpServletResponse httpResponse, String filename, final long filesize)
			throws IOException {
		httpResponse.reset();
		httpResponse.setContentType("bin");
		if (filesize > 0) {
			httpResponse.setHeader("Content-Length", String.valueOf(filesize));
		}
		try {
			if (Browser.get(httpRequest).isGecko()) {
				filename = MimeUtility.encodeText(filename);
			} else {
				filename = URLEncoder.encode(filename, "UTF-8");
				filename = StringUtils.replace(filename, "+", "%20");
			}
			httpResponse.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		} catch (final UnsupportedEncodingException e) {
		}
		return httpResponse.getOutputStream();
	}

	/****************************** parameters *****************************/

	public static String toLocaleString(final String str) {
		return toLocaleString(str, null);
	}

	public static String toLocaleString(final String str, final String charset) {
		if (str == null) {
			return null;
		}
		try {
			final byte[] bytes = str.getBytes("ISO-8859-1");
			if (TextUtils.isAscii(bytes)) {
				return str;
			} else if (TextUtils.isUTF8(bytes)) {
				return new String(bytes, "UTF-8");
			} else if (charset != null) {
				return new String(bytes, charset);
			} else {
				return new String(bytes);
			}
		} catch (final UnsupportedEncodingException e) {
		}
		return str;
	}

	@SuppressWarnings("unchecked")
	public static void putParameter(final HttpServletRequest request, final String key,
			final Object value) {
		if (!StringUtils.hasText(key) || value == null) {
			return;
		}
		String[] sVal;
		if (value.getClass().isArray()) {
			final ArrayList<String> al = new ArrayList<String>();
			final int l = Array.getLength(value);
			for (int i = 0; i < l; i++) {
				al.add(Convert.toString(Array.get(value, i)));
			}
			sVal = al.toArray(new String[al.size()]);
		} else {
			sVal = new String[] { Convert.toString(value) };
		}
		request.getParameterMap().put(key, sVal);
	}

	public static Map<String, String> toQueryParams(final String queryString) {
		return toQueryParams(queryString, null);
	}

	public static Map<String, String> toQueryParams(String queryString, final String charset) {
		final Map<String, String> map = new ParameterMap();
		if (!StringUtils.hasText(queryString)) {
			return map;
		}
		final int pos = queryString.indexOf("?");
		if (pos > -1) {
			queryString = queryString.substring(pos + 1);
		}
		for (final String param : StringUtils.split(queryString, "&")) {
			final String[] tmpArr = StringUtils.split(param, "=");
			if (tmpArr.length == 1) {
				map.put(tmpArr[0], "");
			} else {
				try {
					map.put(tmpArr[0],
							charset == null ? tmpArr[1] : URLDecoder.decode(tmpArr[1], charset));
				} catch (final UnsupportedEncodingException e) {
				}
			}
		}
		return map;
	}

	public static String toQueryString(final Map<String, String> params) {
		return toQueryString(params, null);
	}

	public static String toQueryString(final Map<String, String> params, final String charset) {
		final StringBuilder sb = new StringBuilder();
		if (params != null) {
			for (final Entry<String, String> entry : params.entrySet()) {
				if (sb.length() > 0) {
					sb.append("&");
				}
				try {
					sb.append(entry.getKey()).append("=");
					String val = Convert.toString(entry.getValue());
					if (charset != null) {
						val = URLEncoder.encode(val, charset);
					}
					sb.append(val);
				} catch (final UnsupportedEncodingException e) {
				}
			}
		}
		return sb.toString();
	}

	public static String addParameters(final String url, final String queryString) {
		return addParameters(url, queryString, null);
	}

	public static String addParameters(final String url, final String queryString,
			final String charset) {
		return addParameters(url, toQueryParams(queryString, charset), charset);
	}

	public static String addParameters(String url, final Map<String, String> queryMap,
			final String charset) {
		if (url == null) {
			url = "";
		}
		if (queryMap == null || queryMap.size() == 0) {
			return url;
		}
		final int p = url.indexOf("?");
		String request;
		String query;
		if (p > -1) {
			request = url.substring(0, p);
			query = url.substring(p + 1);
		} else {
			final boolean isQueryString = url.indexOf("=") > 0;
			if (isQueryString) {
				request = "";
				query = url;
			} else {
				request = url;
				query = "";
			}
		}
		final Map<String, String> params = toQueryParams(query, charset);
		for (final Entry<String, String> entry : queryMap.entrySet()) {
			params.put(entry.getKey(), entry.getValue());
		}
		String qs = toQueryString(params, charset);
		if (StringUtils.hasText(qs)) {
			qs = "?" + qs;
		}
		return request + qs;
	}

	/****************************** coll *****************************/

	public static Map<String, String> map(final HttpServletRequest request) {
		final Map<String, String> m = new ParameterMap();
		final Enumeration<?> e = request.getParameterNames();
		while (e.hasMoreElements()) {
			final String key = (String) e.nextElement();
			final String val = toLocaleString(request.getParameter(key));
			if (val != null) {
				m.put(key, val);
			}
		}
		return m;
	}
}
