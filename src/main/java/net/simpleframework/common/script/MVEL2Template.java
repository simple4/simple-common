package net.simpleframework.common.script;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.common.AlgorithmUtils;
import net.simpleframework.common.ClassUtils;
import net.simpleframework.common.IoUtils;
import net.simpleframework.lib.org.mvel2.templates.CompiledTemplate;
import net.simpleframework.lib.org.mvel2.templates.SimpleTemplateRegistry;
import net.simpleframework.lib.org.mvel2.templates.TemplateCompiler;
import net.simpleframework.lib.org.mvel2.templates.TemplateRuntime;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class MVEL2Template {
	private final static Map<String, CompiledTemplate> templateCompilerCache;
	static {
		templateCompilerCache = new ConcurrentHashMap<String, CompiledTemplate>();
	}

	private static CompiledTemplate getCompiledTemplate(final String expr) {
		final String digest = AlgorithmUtils.md5Hex(expr);
		CompiledTemplate compiledTemplate = templateCompilerCache.get(digest);
		if (compiledTemplate == null) {
			templateCompilerCache.put(digest,
					compiledTemplate = TemplateCompiler.compileTemplate(expr));
		}
		return compiledTemplate;
	}

	public static String replace(final Map<String, Object> variables, final String expr,
			final Map<String, String> namedTemplates) {
		final CompiledTemplate compiledTemplate = getCompiledTemplate(expr);
		final SimpleTemplateRegistry registry = new SimpleTemplateRegistry();
		if (namedTemplates != null) {
			for (final Map.Entry<String, String> e : namedTemplates.entrySet()) {
				registry.addNamedTemplate(e.getKey(), getCompiledTemplate(e.getValue()));
			}
		}
		return TemplateRuntime.execute(compiledTemplate, null, variables, registry).toString();
	}

	public static String replace(final Map<String, Object> variables, final String expr) {
		return replace(variables, expr, null);
	}

	public static String replace(final Map<String, Object> variables, final Class<?> resourceClazz,
			final String filename) {
		try {
			return replace(
					variables,
					IoUtils.getStringFromInputStream(
							ClassUtils.getResourceAsStream(resourceClazz, filename), "utf-8"));
		} catch (final IOException e) {
			throw ScriptEvalException.of(e);
		}
	}
}
