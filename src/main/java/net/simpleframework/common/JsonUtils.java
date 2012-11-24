package net.simpleframework.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import net.simpleframework.lib.net.minidev.json.JSONArray;
import net.simpleframework.lib.net.minidev.json.JSONObject;
import net.simpleframework.lib.net.minidev.json.JSONValue;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class JsonUtils {
	/*-------------------------------bean-to-json-------------------------------*/

	public static String toJSON(final Map<String, ?> data) {
		return JSONObject.toJSONString(data);
	}

	public static String toJSON(final Collection<?> data) {
		return JSONArray.toJSONString(new ArrayList<Object>(data));
	}

	/*-------------------------------json-to-bean-------------------------------*/

	public static <T> T toObject(final String json, final Class<T> valueType) {
		if (!StringUtils.hasText(json)) {
			return null;
		}
		return JSONValue.parse(json, valueType);
	}

	@SuppressWarnings("unchecked")
	public static Map<String, ?> toMap(final String json) {
		return toObject(json, Map.class);
	}

	public static Collection<?> toCollection(final String json) {
		return toObject(json, Collection.class);
	}
}
