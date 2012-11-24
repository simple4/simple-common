package net.simpleframework.common.html;

import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.simpleframework.common.Convert;
import net.simpleframework.common.StringUtils;
import net.simpleframework.lib.org.jsoup.nodes.Document;
import net.simpleframework.lib.org.jsoup.nodes.Element;
import net.simpleframework.lib.org.jsoup.nodes.Node;
import net.simpleframework.lib.org.jsoup.nodes.TextNode;
import net.simpleframework.lib.org.jsoup.parser.Parser;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class HtmlUtils {

	public static Document createHtmlDocument(final String htmlString, final boolean wrapHtml) {
		final Document document = new Document("");
		final Element html = document.createElement("html");
		final List<Node> nodeList = Parser.parseFragment(StringUtils.blank(htmlString), html,
				document.baseUri());
		for (final Node node : nodeList.toArray(new Node[nodeList.size()])) {
			if (wrapHtml) {
				html.appendChild(node);
			} else {
				document.appendChild(node);
			}
		}
		if (wrapHtml) {
			document.appendChild(html);
			if (!StringUtils.hasText(html.attr("xmlns"))) {
				html.attr("xmlns", "http://www.w3.org/1999/xhtml");
			}
			Element head = html.select(">head").first();
			if (head == null) {
				html.prependChild(head = document.createElement("head"));
			}
		}
		document.outputSettings().prettyPrint(false);
		return document;
	}

	public static String truncateHtml(final String htmlString, final int length,
			final boolean newLine) {
		return truncateHtml(createHtmlDocument(htmlString, false), length, newLine);
	}

	public static String truncateHtml(final Document doc, final int length, final boolean newLine) {
		return truncateHtml(doc, length, newLine, true, false);
	}

	public static String truncateHtml(final Document doc, final int length, final boolean newLine,
			final boolean showLink, final boolean dot) {
		doc.attr("length", String.valueOf(length));
		String html = elementText(doc, doc.childNodes(), newLine, showLink);
		if (dot) {
			if (Convert.toInt(doc.attr("length")) <= 0) {
				html += "...";
			}
		}
		doc.removeAttr("length");
		doc.removeAttr("br");
		return html;
	}

	private static String elementText(final Document doc, final List<Node> nodes,
			final boolean newLine, final boolean showLink) {
		final StringBuilder sb = new StringBuilder();
		for (final Node child : nodes) {
			final int length = Convert.toInt(doc.attr("length"));
			if (length <= 0) {
				break;
			}
			if (child instanceof TextNode) {
				final String txt = ((TextNode) child).text();
				if (StringUtils.hasText(txt)) {
					sb.append(HtmlEncoder.text(StringUtils.substring(txt, length)));
					doc.attr("length", String.valueOf(length - txt.length()));
					doc.removeAttr("br");
				}
			} else if (child instanceof Element) {
				final Element element = (Element) child;
				String href;
				if (showLink && "a".equalsIgnoreCase(element.tagName())
						&& element.children().size() == 0
						&& StringUtils.hasText(href = element.attr("href"))
						&& !href.toLowerCase().startsWith("javascript:")) {
					doc.attr("length", String.valueOf(length - element.text().length()));
					element.removeAttr("style").removeAttr("class").attr("target", "_blank");
					sb.append(element.outerHtml());
				} else {
					String txt = elementText(doc, element.childNodes(), newLine, showLink);
					txt = StringUtils.replace(txt, "&nbsp;", " ").trim();
					if (StringUtils.hasText(txt)) {
						sb.append(txt);
						if (newLine && element.isBlock() && Convert.toInt(doc.attr("length")) > 0) {
							if (!doc.attr("br").equals("true")) {
								sb.append("<br style=\"margin-bottom: 4px;\" />");
								doc.attr("br", "true");
							}
						}
					}
				}
			}
		}
		return sb.toString();
	}

	private static final Pattern EXPR_PATTERN = Pattern
			.compile("<script[^>]*>([\\S\\s]*?)</script>");

	public static String stripScripts(final String content) {
		if (StringUtils.hasText(content)) {
			return EXPR_PATTERN.matcher(content).replaceAll("");
		} else {
			return StringUtils.blank(content);
		}
	}

	public static final String convertHtmlLines(final String input) {
		return StringUtils.replace(StringUtils.replace(StringUtils.blank(input), "\n", "<br/>"),
				"\r", "").replace("\t", "&nbsp;&nbsp;");
	}

	public static boolean hasTag(final String input) {
		for (final Object o : Parser.parseFragment(StringUtils.blank(input), new Document(""), "")) {
			if (!(o instanceof TextNode)) {
				return true;
			}
		}
		return false;
	}

	private static Pattern url_pattern;

	public static String autoLink(final String txt) {
		if (url_pattern == null) {
			url_pattern = Pattern.compile("(http(s)?|ftp)://([\\w-]+\\.)+[\\w-]+(/[\\w-./?%&=]*)?");
		}
		final StringBuilder html = new StringBuilder();
		int lastIdx = 0;
		final Matcher matchr = url_pattern.matcher(txt);
		while (matchr.find()) {
			final String str = matchr.group();
			html.append(txt.substring(lastIdx, matchr.start()));
			html.append("<a target=\"_blank\" href=\"");
			html.append(str).append("\">").append(str).append("</a>");
			lastIdx = matchr.end();
		}
		html.append(txt.substring(lastIdx));
		return html.toString();
	}

	public static String tag(final String tagName, final Properties properties) {
		return tag(tagName, null, properties);
	}

	public static String tag(final String tagName, final String text, final Properties properties) {
		if (!StringUtils.hasText(tagName)) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		sb.append("<").append(tagName);
		if (properties != null) {
			for (final Entry<Object, Object> entry : properties.entrySet()) {
				sb.append(" ").append(entry.getKey()).append("=\"");
				sb.append(HtmlEncoder.text((String) entry.getValue()));
				sb.append("\"");
			}
		}
		if (text != null) {
			sb.append(">");
			sb.append(text).append("</").append(tagName).append(">");
		} else {
			sb.append("/>");
		}
		return sb.toString();
	}
}
