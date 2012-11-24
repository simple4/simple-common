package net.simpleframework.lib.org.jsoup.parser;

/**
 * A Parse Error records an error in the input HTML that occurs in either the
 * tokenisation or the tree building phase.
 */
public class ParseError {
	private final int pos;
	private final String errorMsg;

	ParseError(final int pos, final String errorMsg) {
		this.pos = pos;
		this.errorMsg = errorMsg;
	}

	ParseError(final int pos, final String errorFormat, final Object... args) {
		this.errorMsg = String.format(errorFormat, args);
		this.pos = pos;
	}

	/**
	 * Retrieve the error message.
	 * 
	 * @return the error message.
	 */
	public String getErrorMessage() {
		return errorMsg;
	}

	/**
	 * Retrieves the offset of the error.
	 * 
	 * @return error offset within input
	 */
	public int getPosition() {
		return pos;
	}

	@Override
	public String toString() {
		return pos + ": " + errorMsg;
	}
}
