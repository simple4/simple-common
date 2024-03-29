package net.simpleframework.common.script;

import java.io.IOException;
import java.util.Map;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.simpleframework.common.ClassUtils;
import net.simpleframework.common.IoUtils;
import net.simpleframework.common.StringUtils;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ScriptEvalUtils {

	static String trimScript(String script) {
		if (script == null) {
			return null;
		}
		script = script.trim();
		final char[] charArray = script.toCharArray();
		final int length = charArray.length;
		if (length > 3 && charArray[0] == '$' && charArray[1] == '{' && charArray[length - 1] == '}') {
			script = script.substring(2, script.length() - 1);
		}
		return script;
	}

	private static final Pattern EXPR_PATTERN = Pattern.compile("[\\s\\S]*(\\$\\{.+\\})[\\s\\S]*");

	public static String replaceExpr(final IScriptEval scriptEval, String expr) {
		if (StringUtils.hasText(expr)) {
			if (scriptEval == null) {
				return expr;
			}
			while (true) {
				final Matcher matcher = EXPR_PATTERN.matcher(expr);
				if (matcher.matches()) {
					final MatchResult result = matcher.toMatchResult();
					final String group = result.group(1);
					expr = expr.substring(0, result.start(1))
							+ StringUtils.blank(scriptEval.eval(group)) + expr.substring(result.end(1));
				} else {
					break;
				}
			}
			return expr;
		}
		return "";
	}

	public static String replaceExpr(final Map<String, Object> variables, final String expr) {
		return replaceExpr(ScriptEvalFactory.createDefaultScriptEval(variables), expr);
	}

	public static String replaceExpr(final Map<String, Object> variables,
			final Class<?> resourceClazz, final String filename) {
		try {
			return replaceExpr(
					variables,
					IoUtils.getStringFromInputStream(
							ClassUtils.getResourceAsStream(resourceClazz, filename), "utf-8"));
		} catch (final IOException e) {
			throw ScriptEvalException.of(e);
		}
	}
}
