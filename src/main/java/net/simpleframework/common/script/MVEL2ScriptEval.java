package net.simpleframework.common.script;

import java.util.Map;

import net.simpleframework.lib.org.mvel2.MVELInterpretedRuntime;
import net.simpleframework.lib.org.mvel2.integration.impl.MapVariableResolverFactory;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class MVEL2ScriptEval implements IScriptEval {

	private final MapVariableResolverFactory variableResolverFactory;

	public MVEL2ScriptEval(final Map<String, Object> variables) {
		variableResolverFactory = variables == null ? new MapVariableResolverFactory()
				: new MapVariableResolverFactory(variables);
	}

	@Override
	public void putVariable(final String key, final Object value) {
		variableResolverFactory.createVariable(key, value);
	}

	private MVELInterpretedRuntime lastrt;

	@Override
	public Object eval(String script) {
		if (script == null) {
			return null;
		}
		script = ScriptEvalUtils.trimScript(script);
		lastrt = new MVELInterpretedRuntime(script, lastrt, variableResolverFactory);
		return lastrt.parse();
	}
}
