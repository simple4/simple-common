package net.simpleframework.common.mail;

import java.util.regex.Pattern;

public class EmailAddress {
	/**
	 * This constant states that domain literals are allowed in the email
	 * address, e.g.:
	 * <p>
	 * <p>
	 * <tt>someone@[192.168.1.100]</tt> or <br/>
	 * <tt>john.doe@[23:33:A2:22:16:1F]</tt> or <br/>
	 * <tt>me@[my computer]</tt>
	 * </p>
	 * <p>
	 * <p>
	 * The RFC says these are valid email addresses, but most people don't like
	 * allowing them. If you don't want to allow them, and only want to allow
	 * valid domain names (<a href="http://www.ietf.org/rfc/rfc1035.txt">RFC
	 * 1035</a>, x.y.z.com, etc), change this constant to <tt>false</tt>.
	 * <p>
	 * <p>
	 * Its default value is <tt>true</tt> to remain RFC 2822 compliant, but you
	 * should set it depending on what you need for your application.
	 */
	private static final boolean ALLOW_DOMAIN_LITERALS = true;

	/**
	 * This constant states that quoted identifiers are allowed (using quotes and
	 * angle brackets around the raw address) are allowed, e.g.:
	 * <p>
	 * <p>
	 * <tt>"John Smith" &lt;john.smith@somewhere.com&gt;</tt>
	 * <p>
	 * <p>
	 * The RFC says this is a valid mailbox. If you don't want to allow this,
	 * because for example, you only want users to enter in a raw address (
	 * <tt>john.smith@somewhere.com</tt> - no quotes or angle brackets), then
	 * change this constant to <tt>false</tt>.
	 * <p>
	 * <p>
	 * Its default value is <tt>true</tt> to remain RFC 2822 compliant, but you
	 * should set it depending on what you need for your application.
	 */
	private static final boolean ALLOW_QUOTED_IDENTIFIERS = true;

	// RFC 2822 2.2.2 Structured Header Field Bodies
	private static final String wsp = "[ \\t]"; // space or tab
	private static final String fwsp = wsp + '*';

	// RFC 2822 3.2.1 Primitive tokens
	private static final String dquote = "\\\"";
	// ASCII Control characters excluding white space:
	private static final String noWsCtl = "\\x01-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F";
	// all ASCII characters except CR and LF:
	private static final String asciiText = "[\\x01-\\x09\\x0B\\x0C\\x0E-\\x7F]";

	// RFC 2822 3.2.2 Quoted characters:
	// single backslash followed by a text char
	private static final String quotedPair = "(\\\\" + asciiText + ')';

	// RFC 2822 3.2.4 Atom:
	private static final String atext = "[a-zA-Z0-9\\!\\#\\$\\%\\&\\'\\*\\+\\-\\/\\=\\?\\^\\_\\`\\{\\|\\}\\~]";
	private static final String atom = fwsp + atext + '+' + fwsp;
	private static final String dotAtomText = atext + '+' + '(' + "\\." + atext + "+)*";
	private static final String dotAtom = fwsp + '(' + dotAtomText + ')' + fwsp;

	// RFC 2822 3.2.5 Quoted strings:
	// noWsCtl and the rest of ASCII except the doublequote and backslash
	// characters:
	private static final String qtext = '[' + noWsCtl + "\\x21\\x23-\\x5B\\x5D-\\x7E]";
	private static final String qcontent = '(' + qtext + '|' + quotedPair + ')';
	private static final String quotedString = dquote + '(' + fwsp + qcontent + ")*" + fwsp + dquote;

	// RFC 2822 3.2.6 Miscellaneous tokens
	private static final String word = "((" + atom + ")|(" + quotedString + "))";
	private static final String phrase = word + '+'; // one or more words.

	// RFC 1035 tokens for domain names:
	private static final String letter = "[a-zA-Z]";
	private static final String letDig = "[a-zA-Z0-9]";
	private static final String letDigHyp = "[a-zA-Z0-9-]";
	private static final String rfcLabel = letDig + '(' + letDigHyp + "{0,61}" + letDig + ")?";
	private static final String rfc1035DomainName = rfcLabel + "(\\." + rfcLabel + ")*\\." + letter
			+ "{2,6}";

	// RFC 2822 3.4 Address specification
	// domain text - non white space controls and the rest of ASCII chars not
	// including [, ], or \:
	private static final String dtext = '[' + noWsCtl + "\\x21-\\x5A\\x5E-\\x7E]";
	private static final String dcontent = dtext + '|' + quotedPair;
	private static final String domainLiteral = "\\[" + '(' + fwsp + dcontent + "+)*" + fwsp + "\\]";
	private static final String rfc2822Domain = '(' + dotAtom + '|' + domainLiteral + ')';

	private static final String domain = ALLOW_DOMAIN_LITERALS ? rfc2822Domain : rfc1035DomainName;

	private static final String localPart = "((" + dotAtom + ")|(" + quotedString + "))";
	private static final String addrSpec = localPart + '@' + domain;
	private static final String angleAddr = '<' + addrSpec + '>';
	private static final String nameAddr = '(' + phrase + ")?" + fwsp + angleAddr;
	private static final String mailbox = nameAddr + '|' + addrSpec;

	// now compile a pattern for efficient re-use:
	// if we're allowing quoted identifiers or not:
	private static final String patternString = ALLOW_QUOTED_IDENTIFIERS ? mailbox : addrSpec;
	public static final Pattern VALID_PATTERN = Pattern.compile(patternString);

	// class attributes
	private String text;
	private boolean bouncing = true;
	private boolean verified;
	private String label;

	public EmailAddress() {
		super();
	}

	public EmailAddress(final String text) {
		super();
		setText(text);
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
	}

	/**
	 * Returns whether or not any emails sent to this email address come back as
	 * bounced (undeliverable).
	 * <p>
	 * <p>
	 * Default is <tt>false</tt> for convenience's sake - if a bounced message is
	 * ever received for this address, this value should be set to <tt>true</tt>
	 * until verification can made.
	 * 
	 * @return whether or not any emails sent to this email address come back as
	 *         bounced (undeliverable).
	 */
	public boolean isBouncing() {
		return bouncing;
	}

	public void setBouncing(final boolean bouncing) {
		this.bouncing = bouncing;
	}

	/**
	 * Returns whether or not the party associated with this email has verified
	 * that it is their email address.
	 * <p>
	 * <p>
	 * Verification is usually done by sending an email to this address and
	 * waiting for the party to respond or click a specific link in the email.
	 * <p>
	 * <p>
	 * Default is <tt>false</tt>.
	 * 
	 * @return whether or not the party associated with this email has verified
	 *         that it is their email address.
	 */
	public boolean isVerified() {
		return verified;
	}

	public void setVerified(final boolean verified) {
		this.verified = verified;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(final String label) {
		this.label = label;
	}

	public boolean isValid() {
		return isValidText(getText());
	}

	public static boolean isValidText(final String email) {
		return (email != null) && VALID_PATTERN.matcher(email).matches();
	}

	@Override
	public boolean equals(final Object o) {
		if (o instanceof EmailAddress) {
			final EmailAddress ea = (EmailAddress) o;
			return getText().equals(ea.getText());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return getText().hashCode();
	}

	@Override
	public String toString() {
		return getText();
	}
}
