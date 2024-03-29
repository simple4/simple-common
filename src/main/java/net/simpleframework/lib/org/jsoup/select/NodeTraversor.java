package net.simpleframework.lib.org.jsoup.select;

import net.simpleframework.lib.org.jsoup.nodes.Node;

/**
 * Depth-first node traversor. Use to iterate through all nodes under and
 * including the specified root node.
 * <p/>
 * This implementation does not use recursion, so a deep DOM does not risk
 * blowing the stack.
 */
public class NodeTraversor {
	private final NodeVisitor visitor;

	/**
	 * Create a new traversor.
	 * 
	 * @param visitor
	 *           a class implementing the {@link NodeVisitor} interface, to be
	 *           called when visiting each node.
	 */
	public NodeTraversor(final NodeVisitor visitor) {
		this.visitor = visitor;
	}

	/**
	 * Start a depth-first traverse of the root and all of its descendants.
	 * 
	 * @param root
	 *           the root node point to traverse.
	 */
	public void traverse(final Node root) {
		Node node = root;
		int depth = 0;

		while (node != null) {
			visitor.head(node, depth);
			if (node.childNodes().size() > 0) {
				node = node.childNode(0);
				depth++;
			} else {
				while (node.nextSibling() == null && depth > 0) {
					visitor.tail(node, depth);
					node = node.parent();
					depth--;
				}
				visitor.tail(node, depth);
				if (node == root) {
					break;
				}
				node = node.nextSibling();
			}
		}
	}
}
