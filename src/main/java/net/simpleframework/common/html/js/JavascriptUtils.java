package net.simpleframework.common.html.js;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.StringWriter;

import net.simpleframework.common.IoUtils;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.logger.Log;
import net.simpleframework.common.logger.LogFactory;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class JavascriptUtils {
	static Log log = LogFactory.getLogger(JavascriptUtils.class);

	public static String escape(final String input) {
		if (input == null) {
			return input;
		}
		final StringBuilder filtered = new StringBuilder(input.length());
		char prevChar = '\u0000';
		char c;
		for (int i = 0; i < input.length(); i++) {
			c = input.charAt(i);
			if (c == '"') {
				filtered.append("\\\"");
			} else if (c == '\'') {
				filtered.append("\\'");
			} else if (c == '\\') {
				filtered.append("\\\\");
			} else if (c == '/') {
				filtered.append("\\/");
			} else if (c == '\t') {
				filtered.append("\\t");
			} else if (c == '\n') {
				if (prevChar != '\r') {
					filtered.append("\\n");
				}
			} else if (c == '\r') {
				filtered.append("\\n");
			} else if (c == '\f') {
				filtered.append("\\f");
			} else {
				filtered.append(c);
			}
			prevChar = c;
		}
		return filtered.toString();
	}

	static boolean compressorEnabled = false;
	static {
		try {
			Class.forName("com.yahoo.platform.yui.compressor.JavaScriptCompressor");
			compressorEnabled = true;
		} catch (final Exception ex) {
			log.warn("Javascript compressor disabled!");
		}
	}

	public static String jsCompress(final String js) {
		if (!compressorEnabled) {
			return js;
		}
		final StringWriter oWriter = new StringWriter();
		try {
			new com.yahoo.platform.yui.compressor.JavaScriptCompressor(new StringReader(js), null)
					.compress(oWriter, -1, true, false, false, false);
			return oWriter.toString();
		} catch (final Throwable e) {
			return js;
		} finally {
			try {
				oWriter.close();
			} catch (final IOException e) {
			}
		}
	}

	public static String wrapWhenReady(final String functionBody) {
		final StringBuilder sb = new StringBuilder();
		sb.append("$ready(function() {").append(StringUtils.blank(functionBody)).append("});");
		return sb.toString();
	}

	public static String wrapFunction(final String functionBody) {
		final StringBuilder sb = new StringBuilder();
		sb.append("(function() {").append(StringUtils.blank(functionBody)).append("})();");
		return sb.toString();
	}

	public static String wrapScriptTag(final String javascript) {
		final StringBuilder sb = new StringBuilder();
		sb.append("<script type=\"text/javascript\">").append(StringUtils.blank(javascript));
		sb.append("</script>");
		return sb.toString();
	}

	public static void copyFile(final InputStream inputStream, final File to,
			final boolean jsCompress, final boolean cssCompress) throws IOException {
		if (!compressorEnabled) {
			IoUtils.copyFile(inputStream, to);
			return;
		}
		final String filename = to.getName();
		if ((jsCompress && filename.endsWith(".js")) || (cssCompress && filename.endsWith(".css"))) {
			OutputStreamWriter oWriter = null;
			String c = null;
			try {
				c = IoUtils.getStringFromInputStream(inputStream);
				if (!to.exists()) {
					IoUtils.createFile(to);
				}
				oWriter = new OutputStreamWriter(new FileOutputStream(to));
				if (filename.endsWith(".js")) {
					new com.yahoo.platform.yui.compressor.JavaScriptCompressor(new StringReader(c), null)
							.compress(oWriter, 200, true, false, false, false);
				} else {
					new com.yahoo.platform.yui.compressor.CssCompressor(new StringReader(c)).compress(
							oWriter, 200);
				}
			} catch (final Exception e) {
				if (c != null) {
					IoUtils.copyFile(new ByteArrayInputStream(c.getBytes()), to);
				}
			} finally {
				if (oWriter != null) {
					oWriter.close();
				}
			}
		} else {
			IoUtils.copyFile(inputStream, to);
		}
	}
}
