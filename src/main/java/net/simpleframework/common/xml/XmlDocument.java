package net.simpleframework.common.xml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.simpleframework.common.IoUtils;
import net.simpleframework.common.ObjectEx;
import net.simpleframework.common.SimpleRuntimeException;

import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class XmlDocument extends ObjectEx {
	protected Document document;

	public XmlDocument(final URL url) throws IOException {
		this(url.openStream());
	}

	public XmlDocument(final InputStream inputStream) {
		createXmlDocument(inputStream);
	}

	public XmlDocument(final Reader reader) {
		createXmlDocument(reader);
	}

	public XmlDocument(final String xml) {
		this(new StringReader(xml));
	}

	protected XmlDocument() {
	}

	protected void createXmlDocument(final Reader reader) {
		try {
			this.document = parse(new InputSource(reader));
			init();
		} catch (final Throwable e) {
			throw XmlDocumentException.of(e);
		}
	}

	protected void createXmlDocument(final InputStream inputStream) {
		try {
			this.document = parse(new InputSource(inputStream));
			init();
		} catch (final Throwable e) {
			try {
				final String xString = stripNonValidXMLCharacters(IoUtils.getStringFromInputStream(
						inputStream, "UTF-8"));
				createXmlDocument(new StringReader(xString));
			} catch (final Throwable e2) {
				throw XmlDocumentException.of(e2);
			}
		}
	}

	private Document parse(final InputSource inputSource) throws ParserConfigurationException,
			SAXException, IOException {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(isNamespaceAware());
		factory.setValidating(isValidating());
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final EntityResolver resolver = getEntityResolver();
		if (resolver != null) {
			builder.setEntityResolver(resolver);
		}
		return builder.parse(inputSource);
	}

	public Document getDocument() {
		return document;
	}

	public XmlElement getRoot() {
		return new XmlElement(document.getDocumentElement());
	}

	protected boolean isValidating() {
		return false;
	}

	protected boolean isNamespaceAware() {
		return false;
	}

	protected EntityResolver getEntityResolver() {
		return null;
	}

	protected void init() throws Exception {
	}

	public String stripNonValidXMLCharacters(final String in) {
		final StringBuilder out = new StringBuilder();
		char current;
		if (in == null || ("".equals(in))) {
			return "";
		}
		for (int i = 0; i < in.length(); i++) {
			current = in.charAt(i);
			if ((current == 0x9) || (current == 0xA) || (current == 0xD)
					|| ((current >= 0x20) && (current <= 0xD7FF))
					|| ((current >= 0xE000) && (current <= 0xFFFD))
					|| ((current >= 0x10000) && (current <= 0x10FFFF))) {
				out.append(current);
			}
		}
		return out.toString();
	}

	@Override
	public String toString() {
		try {
			final Transformer trans = TransformerFactory.newInstance().newTransformer();

			trans.setOutputProperty(OutputKeys.METHOD, "xml");
			trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");

			final StringWriter sWriter = new StringWriter();
			final StreamResult result = new StreamResult(sWriter);
			final DOMSource source = new DOMSource(document);
			trans.transform(source, result);
			return sWriter.toString();
		} catch (final Throwable e) {
			throw XmlDocumentException.of(e);
		}
	}

	public void saveToFile(final File targetFile) throws IOException {
		FileWriter fWriter = null;
		try {
			fWriter = new FileWriter(targetFile);
			fWriter.write(toString());
		} finally {
			if (fWriter != null) {
				fWriter.close();
			}
		}
	}

	public static class XmlDocumentException extends SimpleRuntimeException {
		private static final long serialVersionUID = 3959123626087714493L;

		public XmlDocumentException(final String msg, final Throwable cause) {
			super(msg, cause);
		}

		public static XmlDocumentException of(final Throwable throwable) {
			return _of(XmlDocumentException.class, null, throwable);
		}
	}
}
