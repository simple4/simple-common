package net.simpleframework.common.coll;

import net.simpleframework.common.ID;
import net.simpleframework.common.JsonUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class KVMap extends AbstractKVMap<Object, KVMap> {
	public KVMap(final int initialCapacity) {
		super(initialCapacity);
	}

	public KVMap() {
		super();
	}

	public KVMap(final String json) {
		putAll(JsonUtils.toMap(json));
	}

	@Override
	public Object put(final String key, Object value) {
		if (value instanceof ID) {
			value = ((ID) value).getValue();
		} else if (value instanceof AbstractKVMap) {
			value = ((AbstractKVMap<?, ?>) value).map();
		}
		return super.put(key, value);
	}

	private static final long serialVersionUID = 7340654517376280959L;
}
