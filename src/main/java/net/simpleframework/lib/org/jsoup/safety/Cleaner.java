package net.simpleframework.lib.org.jsoup.safety;

import java.util.List;

import net.simpleframework.lib.org.jsoup.helper.Validate;
import net.simpleframework.lib.org.jsoup.nodes.Attribute;
import net.simpleframework.lib.org.jsoup.nodes.Attributes;
import net.simpleframework.lib.org.jsoup.nodes.Document;
import net.simpleframework.lib.org.jsoup.nodes.Element;
import net.simpleframework.lib.org.jsoup.nodes.Node;
import net.simpleframework.lib.org.jsoup.nodes.TextNode;
import net.simpleframework.lib.org.jsoup.parser.Tag;

/**
 * The whitelist based HTML cleaner. Use to ensure that end-user provided HTML
 * contains only the elements and attributes that you are expecting; no junk,
 * and no cross-site scripting attacks!
 * <p/>
 * The HTML cleaner parses the input as HTML and then runs it through a
 * white-list, so the output HTML can only contain HTML that is allowed by the
 * whitelist.
 * <p/>
 * It is assumed that the input HTML is a body fragment; the clean methods only
 * pull from the source's body, and the canned white-lists only allow body
 * contained tags.
 * <p/>
 * Rather than interacting directly with a Cleaner object, generally see the
 * {@code clean} methods in {@link org.jsoup.Jsoup}.
 */
public class Cleaner {
	private final Whitelist whitelist;

	/**
	 * Create a new cleaner, that sanitizes documents using the supplied
	 * whitelist.
	 * 
	 * @param whitelist
	 *           white-list to clean with
	 */
	public Cleaner(final Whitelist whitelist) {
		Validate.notNull(whitelist);
		this.whitelist = whitelist;
	}

	/**
	 * Creates a new, clean document, from the original dirty document,
	 * containing only elements allowed by the whitelist. The original document
	 * is not modified. Only elements from the dirt document's <code>body</code>
	 * are used.
	 * 
	 * @param dirtyDocument
	 *           Untrusted base document to clean.
	 * @return cleaned document.
	 */
	public Document clean(final Document dirtyDocument) {
		Validate.notNull(dirtyDocument);

		final Document clean = Document.createShell(dirtyDocument.baseUri());
		if (dirtyDocument.body() != null) {
			// the clean doc will have empty body.
			copySafeNodes(dirtyDocument.body(), clean.body());
		}

		return clean;
	}

	/**
	 * Determines if the input document is valid, against the whitelist. It is
	 * considered valid if all the tags and attributes in the input HTML are
	 * allowed by the whitelist.
	 * <p/>
	 * This method can be used as a validator for user input forms. An invalid
	 * document will still be cleaned successfully using the
	 * {@link #clean(Document)} document. If using as a validator, it is
	 * recommended to still clean the document to ensure enforced attributes are
	 * set correctly, and that the output is tidied.
	 * 
	 * @param dirtyDocument
	 *           document to test
	 * @return true if no tags or attributes need to be removed; false if they do
	 */
	public boolean isValid(final Document dirtyDocument) {
		Validate.notNull(dirtyDocument);

		final Document clean = Document.createShell(dirtyDocument.baseUri());
		final int numDiscarded = copySafeNodes(dirtyDocument.body(), clean.body());
		return numDiscarded == 0;
	}

	/**
	 * Iterates the input and copies trusted nodes (tags, attributes, text) into
	 * the destination.
	 * 
	 * @param source
	 *           source of HTML
	 * @param dest
	 *           destination element to copy into
	 * @return number of discarded elements (that were considered unsafe)
	 */
	private int copySafeNodes(final Element source, final Element dest) {
		final List<Node> sourceChildren = source.childNodes();
		int numDiscarded = 0;

		for (final Node sourceChild : sourceChildren) {
			if (sourceChild instanceof Element) {
				final Element sourceEl = (Element) sourceChild;

				if (whitelist.isSafeTag(sourceEl.tagName())) { // safe, clone and
																				// copy safe attrs
					final ElementMeta meta = createSafeElement(sourceEl);
					final Element destChild = meta.el;
					dest.appendChild(destChild);

					numDiscarded += meta.numAttribsDiscarded;
					numDiscarded += copySafeNodes(sourceEl, destChild); // recurs
				} else { // not a safe tag, but it may have children (els or text)
							// that are, so recurse
					numDiscarded++;
					numDiscarded += copySafeNodes(sourceEl, dest);
				}
			} else if (sourceChild instanceof TextNode) {
				final TextNode sourceText = (TextNode) sourceChild;
				final TextNode destText = new TextNode(sourceText.getWholeText(), sourceChild.baseUri());
				dest.appendChild(destText);
			} // else, we don't care about comments, xml proc instructions, etc
		}
		return numDiscarded;
	}

	private ElementMeta createSafeElement(final Element sourceEl) {
		final String sourceTag = sourceEl.tagName();
		final Attributes destAttrs = new Attributes();
		final Element dest = new Element(Tag.valueOf(sourceTag), sourceEl.baseUri(), destAttrs);
		int numDiscarded = 0;

		final Attributes sourceAttrs = sourceEl.attributes();
		for (final Attribute sourceAttr : sourceAttrs) {
			if (whitelist.isSafeAttribute(sourceTag, sourceEl, sourceAttr)) {
				destAttrs.put(sourceAttr);
			} else {
				numDiscarded++;
			}
		}
		final Attributes enforcedAttrs = whitelist.getEnforcedAttributes(sourceTag);
		destAttrs.addAll(enforcedAttrs);

		return new ElementMeta(dest, numDiscarded);
	}

	private static class ElementMeta {
		Element el;
		int numAttribsDiscarded;

		ElementMeta(final Element el, final int numAttribsDiscarded) {
			this.el = el;
			this.numAttribsDiscarded = numAttribsDiscarded;
		}
	}

}
