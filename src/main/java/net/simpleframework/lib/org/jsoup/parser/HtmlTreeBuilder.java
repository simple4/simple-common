package net.simpleframework.lib.org.jsoup.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.simpleframework.lib.org.jsoup.helper.DescendableLinkedList;
import net.simpleframework.lib.org.jsoup.helper.StringUtil;
import net.simpleframework.lib.org.jsoup.helper.Validate;
import net.simpleframework.lib.org.jsoup.nodes.Comment;
import net.simpleframework.lib.org.jsoup.nodes.DataNode;
import net.simpleframework.lib.org.jsoup.nodes.Document;
import net.simpleframework.lib.org.jsoup.nodes.Element;
import net.simpleframework.lib.org.jsoup.nodes.Node;
import net.simpleframework.lib.org.jsoup.nodes.TextNode;

/**
 * HTML Tree Builder; creates a DOM from Tokens.
 */
class HtmlTreeBuilder extends TreeBuilder {

	private HtmlTreeBuilderState state; // the current state
	private HtmlTreeBuilderState originalState; // original / marked state

	private boolean baseUriSetFromDoc = false;
	private Element headElement; // the current head element
	private Element formElement; // the current form element
	private Element contextElement; // fragment parse context -- could be null
												// even if fragment parsing
	private final DescendableLinkedList<Element> formattingElements = new DescendableLinkedList<Element>(); // active
	// (open)
	// formatting
	// elements
	private List<Token.Character> pendingTableCharacters = new ArrayList<Token.Character>(); // chars
																															// in
																															// table
																															// to
																															// be
																															// shifted
																															// out

	private boolean framesetOk = true; // if ok to go into frameset
	private boolean fosterInserts = false; // if next inserts should be fostered
	private boolean fragmentParsing = false; // if parsing a fragment of html

	HtmlTreeBuilder() {
	}

	@Override
	Document parse(final String input, final String baseUri, final ParseErrorList errors) {
		state = HtmlTreeBuilderState.Initial;
		return super.parse(input, baseUri, errors);
	}

	List<Node> parseFragment(final String inputFragment, final Element context,
			final String baseUri, final ParseErrorList errors) {
		// context may be null
		state = HtmlTreeBuilderState.Initial;
		initialiseParse(inputFragment, baseUri, errors);
		contextElement = context;
		fragmentParsing = true;
		Element root = null;

		if (context != null) {
			if (context.ownerDocument() != null) {
				doc.quirksMode(context.ownerDocument().quirksMode());
			}

			// initialise the tokeniser state:
			final String contextTag = context.tagName();
			if (StringUtil.in(contextTag, "title", "textarea")) {
				tokeniser.transition(TokeniserState.Rcdata);
			} else if (StringUtil.in(contextTag, "iframe", "noembed", "noframes", "style", "xmp")) {
				tokeniser.transition(TokeniserState.Rawtext);
			} else if (contextTag.equals("script")) {
				tokeniser.transition(TokeniserState.ScriptData);
			} else if (contextTag.equals(("noscript"))) {
				tokeniser.transition(TokeniserState.Data); // if scripting enabled,
			} else if (contextTag.equals("plaintext")) {
				tokeniser.transition(TokeniserState.Data);
			} else {
				tokeniser.transition(TokeniserState.Data); // default
			}

			root = new Element(Tag.valueOf("html"), baseUri);
			doc.appendChild(root);
			stack.push(root);
			resetInsertionMode();
			// todo: setup form element to nearest form on context (up ancestor
			// chain)
		}

		runParser();
		if (context != null) {
			return root.childNodes();
		} else {
			return doc.childNodes();
		}
	}

	@Override
	protected boolean process(final Token token) {
		currentToken = token;
		return this.state.process(token, this);
	}

	boolean process(final Token token, final HtmlTreeBuilderState state) {
		currentToken = token;
		return state.process(token, this);
	}

	void transition(final HtmlTreeBuilderState state) {
		this.state = state;
	}

	HtmlTreeBuilderState state() {
		return state;
	}

	void markInsertionMode() {
		originalState = state;
	}

	HtmlTreeBuilderState originalState() {
		return originalState;
	}

	void framesetOk(final boolean framesetOk) {
		this.framesetOk = framesetOk;
	}

	boolean framesetOk() {
		return framesetOk;
	}

	Document getDocument() {
		return doc;
	}

	String getBaseUri() {
		return baseUri;
	}

	void maybeSetBaseUri(final Element base) {
		if (baseUriSetFromDoc) {
			return;
		}

		final String href = base.absUrl("href");
		if (href.length() != 0) { // ignore <base target> etc
			baseUri = href;
			baseUriSetFromDoc = true;
			doc.setBaseUri(href); // set on the doc so doc.createElement(Tag) will
											// get updated base, and to update all
											// descendants
		}
	}

	boolean isFragmentParsing() {
		return fragmentParsing;
	}

	void error(final HtmlTreeBuilderState state) {
		if (errors.canAddError()) {
			errors.add(new ParseError(reader.pos(), "Unexpected token [%s] when in state [%s]",
					currentToken.tokenType(), state));
		}
	}

	Element insert(final Token.StartTag startTag) {
		// handle empty unknown tags
		// when the spec expects an empty tag, will directly hit insertEmpty, so
		// won't generate fake end tag.
		if (startTag.isSelfClosing() && !Tag.isKnownTag(startTag.name())) {
			final Element el = insertEmpty(startTag);
			process(new Token.EndTag(el.tagName())); // ensure we get out of
																	// whatever state we are in
			return el;
		}

		final Element el = new Element(Tag.valueOf(startTag.name()), baseUri, startTag.attributes);
		insert(el);
		return el;
	}

	Element insert(final String startTagName) {
		final Element el = new Element(Tag.valueOf(startTagName), baseUri);
		insert(el);
		return el;
	}

	void insert(final Element el) {
		insertNode(el);
		stack.add(el);
	}

	Element insertEmpty(final Token.StartTag startTag) {
		final Tag tag = Tag.valueOf(startTag.name());
		final Element el = new Element(tag, baseUri, startTag.attributes);
		insertNode(el);
		if (startTag.isSelfClosing()) {
			tokeniser.acknowledgeSelfClosingFlag();
			if (!tag.isKnownTag()) {
				// for output
				tag.setSelfClosing();
			}
		}
		return el;
	}

	void insert(final Token.Comment commentToken) {
		final Comment comment = new Comment(commentToken.getData(), baseUri);
		insertNode(comment);
	}

	void insert(final Token.Character characterToken) {
		Node node;
		// characters in script and style go in as datanodes, not text nodes
		if (StringUtil.in(currentElement().tagName(), "script", "style")) {
			node = new DataNode(characterToken.getData(), baseUri);
		} else {
			node = new TextNode(characterToken.getData(), baseUri);
		}
		currentElement().appendChild(node); // doesn't use insertNode, because we
														// don't foster these; and will always
														// have a stack.
	}

	private void insertNode(final Node node) {
		// if the stack hasn't been set up yet, elements (doctype, comments) go
		// into the doc
		if (stack.size() == 0) {
			doc.appendChild(node);
		} else if (isFosterInserts()) {
			insertInFosterParent(node);
		} else {
			currentElement().appendChild(node);
		}
	}

	Element pop() {
		// todo - dev, remove validation check
		if (stack.peekLast().nodeName().equals("td") && !state.name().equals("InCell")) {
			Validate.isFalse(true, "pop td not in cell");
		}
		if (stack.peekLast().nodeName().equals("html")) {
			Validate.isFalse(true, "popping html!");
		}
		return stack.pollLast();
	}

	void push(final Element element) {
		stack.add(element);
	}

	DescendableLinkedList<Element> getStack() {
		return stack;
	}

	boolean onStack(final Element el) {
		return isElementInQueue(stack, el);
	}

	private boolean isElementInQueue(final DescendableLinkedList<Element> queue,
			final Element element) {
		final Iterator<Element> it = queue.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (next == element) {
				return true;
			}
		}
		return false;
	}

	Element getFromStack(final String elName) {
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (next.nodeName().equals(elName)) {
				return next;
			}
		}
		return null;
	}

	boolean removeFromStack(final Element el) {
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (next == el) {
				it.remove();
				return true;
			}
		}
		return false;
	}

	void popStackToClose(final String elName) {
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (next.nodeName().equals(elName)) {
				it.remove();
				break;
			} else {
				it.remove();
			}
		}
	}

	void popStackToClose(final String... elNames) {
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (StringUtil.in(next.nodeName(), elNames)) {
				it.remove();
				break;
			} else {
				it.remove();
			}
		}
	}

	void popStackToBefore(final String elName) {
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (next.nodeName().equals(elName)) {
				break;
			} else {
				it.remove();
			}
		}
	}

	void clearStackToTableContext() {
		clearStackToContext("table");
	}

	void clearStackToTableBodyContext() {
		clearStackToContext("tbody", "tfoot", "thead");
	}

	void clearStackToTableRowContext() {
		clearStackToContext("tr");
	}

	private void clearStackToContext(final String... nodeNames) {
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (StringUtil.in(next.nodeName(), nodeNames) || next.nodeName().equals("html")) {
				break;
			} else {
				it.remove();
			}
		}
	}

	Element aboveOnStack(final Element el) {
		assert onStack(el);
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (next == el) {
				return it.next();
			}
		}
		return null;
	}

	void insertOnStackAfter(final Element after, final Element in) {
		final int i = stack.lastIndexOf(after);
		Validate.isTrue(i != -1);
		stack.add(i + 1, in);
	}

	void replaceOnStack(final Element out, final Element in) {
		replaceInQueue(stack, out, in);
	}

	private void replaceInQueue(final LinkedList<Element> queue, final Element out, final Element in) {
		final int i = queue.lastIndexOf(out);
		Validate.isTrue(i != -1);
		queue.remove(i);
		queue.add(i, in);
	}

	void resetInsertionMode() {
		boolean last = false;
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			Element node = it.next();
			if (!it.hasNext()) {
				last = true;
				node = contextElement;
			}
			final String name = node.nodeName();
			if ("select".equals(name)) {
				transition(HtmlTreeBuilderState.InSelect);
				break; // frag
			} else if (("td".equals(name) || "td".equals(name) && !last)) {
				transition(HtmlTreeBuilderState.InCell);
				break;
			} else if ("tr".equals(name)) {
				transition(HtmlTreeBuilderState.InRow);
				break;
			} else if ("tbody".equals(name) || "thead".equals(name) || "tfoot".equals(name)) {
				transition(HtmlTreeBuilderState.InTableBody);
				break;
			} else if ("caption".equals(name)) {
				transition(HtmlTreeBuilderState.InCaption);
				break;
			} else if ("colgroup".equals(name)) {
				transition(HtmlTreeBuilderState.InColumnGroup);
				break; // frag
			} else if ("table".equals(name)) {
				transition(HtmlTreeBuilderState.InTable);
				break;
			} else if ("head".equals(name)) {
				transition(HtmlTreeBuilderState.InBody);
				break; // frag
			} else if ("body".equals(name)) {
				transition(HtmlTreeBuilderState.InBody);
				break;
			} else if ("frameset".equals(name)) {
				transition(HtmlTreeBuilderState.InFrameset);
				break; // frag
			} else if ("html".equals(name)) {
				transition(HtmlTreeBuilderState.BeforeHead);
				break; // frag
			} else if (last) {
				transition(HtmlTreeBuilderState.InBody);
				break; // frag
			}
		}
	}

	// todo: tidy up in specific scope methods
	private boolean inSpecificScope(final String targetName, final String[] baseTypes,
			final String[] extraTypes) {
		return inSpecificScope(new String[] { targetName }, baseTypes, extraTypes);
	}

	private boolean inSpecificScope(final String[] targetNames, final String[] baseTypes,
			final String[] extraTypes) {
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element el = it.next();
			final String elName = el.nodeName();
			if (StringUtil.in(elName, targetNames)) {
				return true;
			}
			if (StringUtil.in(elName, baseTypes)) {
				return false;
			}
			if (extraTypes != null && StringUtil.in(elName, extraTypes)) {
				return false;
			}
		}
		Validate.fail("Should not be reachable");
		return false;
	}

	boolean inScope(final String[] targetNames) {
		return inSpecificScope(targetNames, new String[] { "applet", "caption", "html", "table",
				"td", "th", "marquee", "object" }, null);
	}

	boolean inScope(final String targetName) {
		return inScope(targetName, null);
	}

	boolean inScope(final String targetName, final String[] extras) {
		return inSpecificScope(targetName, new String[] { "applet", "caption", "html", "table", "td",
				"th", "marquee", "object" }, extras);
		// todo: in mathml namespace: mi, mo, mn, ms, mtext annotation-xml
		// todo: in svg namespace: forignOjbect, desc, title
	}

	boolean inListItemScope(final String targetName) {
		return inScope(targetName, new String[] { "ol", "ul" });
	}

	boolean inButtonScope(final String targetName) {
		return inScope(targetName, new String[] { "button" });
	}

	boolean inTableScope(final String targetName) {
		return inSpecificScope(targetName, new String[] { "html", "table" }, null);
	}

	boolean inSelectScope(final String targetName) {
		final Iterator<Element> it = stack.descendingIterator();
		while (it.hasNext()) {
			final Element el = it.next();
			final String elName = el.nodeName();
			if (elName.equals(targetName)) {
				return true;
			}
			if (!StringUtil.in(elName, "optgroup", "option")) {
				// except
				return false;
			}
		}
		Validate.fail("Should not be reachable");
		return false;
	}

	void setHeadElement(final Element headElement) {
		this.headElement = headElement;
	}

	Element getHeadElement() {
		return headElement;
	}

	boolean isFosterInserts() {
		return fosterInserts;
	}

	void setFosterInserts(final boolean fosterInserts) {
		this.fosterInserts = fosterInserts;
	}

	Element getFormElement() {
		return formElement;
	}

	void setFormElement(final Element formElement) {
		this.formElement = formElement;
	}

	void newPendingTableCharacters() {
		pendingTableCharacters = new ArrayList<Token.Character>();
	}

	List<Token.Character> getPendingTableCharacters() {
		return pendingTableCharacters;
	}

	void setPendingTableCharacters(final List<Token.Character> pendingTableCharacters) {
		this.pendingTableCharacters = pendingTableCharacters;
	}

	/**
	 * 11.2.5.2 Closing elements that have implied end tags
	 * <p/>
	 * When the steps below require the UA to generate implied end tags, then,
	 * while the current node is a dd element, a dt element, an li element, an
	 * option element, an optgroup element, a p element, an rp element, or an rt
	 * element, the UA must pop the current node off the stack of open elements.
	 * 
	 * @param excludeTag
	 *           If a step requires the UA to generate implied end tags but lists
	 *           an element to exclude from the process, then the UA must perform
	 *           the above steps as if that element was not in the above list.
	 */
	void generateImpliedEndTags(final String excludeTag) {
		while ((excludeTag != null && !currentElement().nodeName().equals(excludeTag))
				&& StringUtil.in(currentElement().nodeName(), "dd", "dt", "li", "option", "optgroup",
						"p", "rp", "rt")) {
			pop();
		}
	}

	void generateImpliedEndTags() {
		generateImpliedEndTags(null);
	}

	boolean isSpecial(final Element el) {
		// todo: mathml's mi, mo, mn
		// todo: svg's foreigObject, desc, title
		final String name = el.nodeName();
		return StringUtil.in(name, "address", "applet", "area", "article", "aside", "base",
				"basefont", "bgsound", "blockquote", "body", "br", "button", "caption", "center",
				"col", "colgroup", "command", "dd", "details", "dir", "div", "dl", "dt", "embed",
				"fieldset", "figcaption", "figure", "footer", "form", "frame", "frameset", "h1", "h2",
				"h3", "h4", "h5", "h6", "head", "header", "hgroup", "hr", "html", "iframe", "img",
				"input", "isindex", "li", "link", "listing", "marquee", "menu", "meta", "nav",
				"noembed", "noframes", "noscript", "object", "ol", "p", "param", "plaintext", "pre",
				"script", "section", "select", "style", "summary", "table", "tbody", "td", "textarea",
				"tfoot", "th", "thead", "title", "tr", "ul", "wbr", "xmp");
	}

	// active formatting elements
	void pushActiveFormattingElements(final Element in) {
		int numSeen = 0;
		final Iterator<Element> iter = formattingElements.descendingIterator();
		while (iter.hasNext()) {
			final Element el = iter.next();
			if (el == null) {
				break;
			}

			if (isSameFormattingElement(in, el)) {
				numSeen++;
			}

			if (numSeen == 3) {
				iter.remove();
				break;
			}
		}
		formattingElements.add(in);
	}

	private boolean isSameFormattingElement(final Element a, final Element b) {
		// same if: same namespace, tag, and attributes. Element.equals only
		// checks tag, might in future check children
		return a.nodeName().equals(b.nodeName()) &&
		// a.namespace().equals(b.namespace()) &&
				a.attributes().equals(b.attributes());
		// todo: namespaces
	}

	void reconstructFormattingElements() {
		final int size = formattingElements.size();
		if (size == 0 || formattingElements.getLast() == null
				|| onStack(formattingElements.getLast())) {
			return;
		}

		Element entry = formattingElements.getLast();
		int pos = size - 1;
		boolean skip = false;
		while (true) {
			if (pos == 0) { // step 4. if none before, skip to 8
				skip = true;
				break;
			}
			entry = formattingElements.get(--pos); // step 5. one earlier than
																// entry
			if (entry == null || onStack(entry)) {
				// stack
				break; // jump to 8, else continue back to 4
			}
		}
		while (true) {
			if (!skip) {
				entry = formattingElements.get(++pos);
			}
			Validate.notNull(entry); // should not occur, as we break at last
												// element

			// 8. create new element from element, 9 insert into current node, onto
			// stack
			skip = false; // can only skip increment from 4.
			final Element newEl = insert(entry.nodeName()); // todo: avoid
																			// fostering
			// here?
			// newEl.namespace(entry.namespace()); // todo: namespaces
			newEl.attributes().addAll(entry.attributes());

			// 10. replace entry with new entry
			formattingElements.add(pos, newEl);
			formattingElements.remove(pos + 1);

			// 11
			if (pos == size - 1) {
				break;
			}
		}
	}

	void clearFormattingElementsToLastMarker() {
		while (!formattingElements.isEmpty()) {
			final Element el = formattingElements.peekLast();
			formattingElements.removeLast();
			if (el == null) {
				break;
			}
		}
	}

	void removeFromActiveFormattingElements(final Element el) {
		final Iterator<Element> it = formattingElements.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (next == el) {
				it.remove();
				break;
			}
		}
	}

	boolean isInActiveFormattingElements(final Element el) {
		return isElementInQueue(formattingElements, el);
	}

	Element getActiveFormattingElement(final String nodeName) {
		final Iterator<Element> it = formattingElements.descendingIterator();
		while (it.hasNext()) {
			final Element next = it.next();
			if (next == null) {
				break;
			} else if (next.nodeName().equals(nodeName)) {
				return next;
			}
		}
		return null;
	}

	void replaceActiveFormattingElement(final Element out, final Element in) {
		replaceInQueue(formattingElements, out, in);
	}

	void insertMarkerToFormattingElements() {
		formattingElements.add(null);
	}

	void insertInFosterParent(final Node in) {
		Element fosterParent = null;
		final Element lastTable = getFromStack("table");
		boolean isLastTableParent = false;
		if (lastTable != null) {
			if (lastTable.parent() != null) {
				fosterParent = lastTable.parent();
				isLastTableParent = true;
			} else {
				fosterParent = aboveOnStack(lastTable);
			}
		} else { // no table == frag
			fosterParent = stack.get(0);
		}

		if (isLastTableParent) {
			Validate.notNull(lastTable); // last table cannot be null by this
													// point.
			lastTable.before(in);
		} else {
			fosterParent.appendChild(in);
		}
	}

	@Override
	public String toString() {
		return "TreeBuilder{" + "currentToken=" + currentToken + ", state=" + state
				+ ", currentElement=" + currentElement() + '}';
	}
}
