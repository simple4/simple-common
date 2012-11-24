package net.simpleframework.common.script;

import java.util.Map;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class ScriptEvalFactory {

	public static IScriptEval createDefaultScriptEval(final Map<String, Object> variables) {
		return new MVEL2ScriptEval(variables);
	}
}
