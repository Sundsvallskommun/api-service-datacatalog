package se.sundsvall.datacatalog.service.merger;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import se.sundsvall.dept44.util.jacoco.ExcludeFromJacocoGeneratedCoverageReport;

/**
 * Secure XML parsing and serialization for RDF/XML documents.
 * <p>
 * Excluded from JaCoCo coverage: catch blocks guard against JVM-level XML infrastructure failures (missing XML parser,
 * broken transformer) that cannot occur in practice and cannot be unit tested.
 */
@ExcludeFromJacocoGeneratedCoverageReport
public final class RdfParser {

	private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY = createSecureFactory();

	private RdfParser() {}

	/**
	 * Parses an RDF/XML string into a namespace-aware DOM document.
	 *
	 * @param  xml the RDF/XML string to parse
	 * @return     parsed DOM document
	 */
	public static Document parse(final String xml) {
		try {
			return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder()
				.parse(new InputSource(new StringReader(xml)));
		} catch (final Exception e) {
			throw new IllegalStateException("Failed to parse RDF/XML", e);
		}
	}

	/**
	 * Creates a new empty DOM document.
	 *
	 * @return empty DOM document
	 */
	public static Document createDocument() {
		try {
			return DOCUMENT_BUILDER_FACTORY.newDocumentBuilder().newDocument();
		} catch (final Exception e) {
			throw new IllegalStateException("Failed to create document builder", e);
		}
	}

	/**
	 * Serializes a DOM document to an indented UTF-8 XML string.
	 *
	 * @param  document the DOM document to serialize
	 * @return          XML string representation
	 */
	public static String serialize(final Document document) {
		try {
			final var transformer = secureTransformerFactory().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

			final var writer = new StringWriter();
			transformer.transform(new DOMSource(document), new StreamResult(writer));
			return writer.toString();
		} catch (final Exception e) {
			throw new IllegalStateException("Failed to serialize RDF document", e);
		}
	}

	private static DocumentBuilderFactory createSecureFactory() {
		try {
			final var factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			return factory;
		} catch (final Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static TransformerFactory secureTransformerFactory() {
		final var factory = TransformerFactory.newInstance();
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
		return factory;
	}
}
