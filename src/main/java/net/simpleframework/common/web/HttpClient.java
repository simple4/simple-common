package net.simpleframework.common.web;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.common.ObjectEx;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.lib.org.jsoup.Connection;
import net.simpleframework.lib.org.jsoup.Connection.Method;
import net.simpleframework.lib.org.jsoup.Jsoup;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class HttpClient extends ObjectEx {
	private static Map<String, HttpClient> cache = new ConcurrentHashMap<String, HttpClient>();

	public static HttpClient of(final String url) {
		HttpClient httpClient = cache.get(url);
		if (httpClient == null) {
			cache.put(url, httpClient = new HttpClient(url));
		}
		return httpClient;
	}

	private String url;

	private String jsessionid;

	private HttpClient(final String url) {
		setUrl(url);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(final String url) {
		if (url != null) {
			this.url = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
		}
	}

	public String getJsessionid() {
		return jsessionid;
	}

	public void setJsessionid(final String jsessionid) {
		this.jsessionid = jsessionid;
	}

	/** --------------- get --------------- **/

	public Map<String, Object> get(final String path, final Map<String, Object> data)
			throws IOException {
		return new KVMap(getResponseText(path, data, false));
	}

	public Map<String, Object> get(final String path) throws IOException {
		return get(path, null);
	}

	/** --------------- post --------------- **/

	public Map<String, Object> post(final String path, final Map<String, Object> data)
			throws IOException {
		return new KVMap(getResponseText(path, data, true));
	}

	public String text() throws IOException {
		return getResponseText(null, null, false);
	}

	protected String getResponseText(final String path, final Map<String, Object> data,
			final boolean post) throws IOException {
		String url = getUrl();
		if (!url.toLowerCase().startsWith("http://")) {
			url = "http://" + url;
		}
		if (StringUtils.hasText(path)) {
			url += path;
		}
		int p;
		final String jsessionid = getJsessionid();
		if (StringUtils.hasText(jsessionid) && (p = url.indexOf("?")) > 0) {
			url = url.substring(0, p) + ";jsessionid=" + jsessionid + url.substring(p);
		}
		final Connection conn = Jsoup.connect(url).userAgent("HttpClient-[service]").timeout(0);
		if (data != null) {
			for (final Map.Entry<String, Object> o : data.entrySet()) {
				conn.data(o.getKey(), String.valueOf(o.getValue()));
			}
		}
		if (post) {
			conn.method(Method.POST);
		}
		return conn.execute().body();
	}
}
