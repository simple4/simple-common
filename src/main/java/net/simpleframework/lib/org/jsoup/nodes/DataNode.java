package net.simpleframework.lib.org.jsoup.nodes;

/**
 * A data node, for contents of style, script tags etc, where contents should
 * not show in text().
 * 
 * @author Jonathan Hedley, jonathan@hedley.net
 */
public class DataNode extends Node {
	private static final String DATA_KEY = "data";

	/**
	 * Create a new DataNode.
	 * 
	 * @param data
	 *           data contents
	 * @param baseUri
	 *           base URI
	 */
	public DataNode(final String data, final String baseUri) {
		super(baseUri);
		attributes.put(DATA_KEY, data);
	}

	@Override
	public String nodeName() {
		return "#data";
	}

	/**
	 * Get the data contents of this node. Will be unescaped and with original
	 * new lines, space etc.
	 * 
	 * @return data
	 */
	public String getWholeData() {
		return attributes.get(DATA_KEY);
	}

	/**
	 * Set the data contents of this node.
	 * 
	 * @param data
	 *           unencoded data
	 * @return this node, for chaining
	 */
	public DataNode setWholeData(final String data) {
		attributes.put(DATA_KEY, data);
		return this;
	}

	@Override
	void outerHtmlHead(final StringBuilder accum, final int depth, final Document.OutputSettings out) {
		accum.append(getWholeData()); // data is not escaped in return from data
												// nodes, so " in script, style is plain
	}

	@Override
	void outerHtmlTail(final StringBuilder accum, final int depth, final Document.OutputSettings out) {
	}

	@Override
	public String toString() {
		return outerHtml();
	}

	/**
	 * Create a new DataNode from HTML encoded data.
	 * 
	 * @param encodedData
	 *           encoded data
	 * @param baseUri
	 *           bass URI
	 * @return new DataNode
	 */
	public static DataNode createFromEncoded(final String encodedData, final String baseUri) {
		final String data = Entities.unescape(encodedData);
		return new DataNode(data, baseUri);
	}
}
