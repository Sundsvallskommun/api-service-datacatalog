package se.sundsvall.datacatalog.service.merger;

import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * DCAT-specific helpers for identifying and extracting RDF elements.
 */
public final class DcatElements {

	public static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static final String DCAT_NS = "http://www.w3.org/ns/dcat#";

	private DcatElements() {}

	/**
	 * Returns {@code true} if the element is a {@code dcat:Catalog}.
	 */
	static boolean isCatalog(final Element element) {
		return hasName(element, DCAT_NS, "Catalog");
	}

	/**
	 * Returns {@code true} if the element is a {@code dcat:dataset} reference.
	 */
	static boolean isDatasetRef(final Element element) {
		return hasName(element, DCAT_NS, "dataset");
	}

	/**
	 * Produces a deduplication key from an element's namespace, local name, and {@code rdf:about} attribute.
	 */
	static String elementKey(final Element element) {
		return Optional.ofNullable(element.getNamespaceURI()).orElse("")
			+ "#" + element.getLocalName()
			+ "#" + element.getAttributeNS(RDF_NS, "about");
	}

	/**
	 * Extracts all {@code dcat:dataset} resource URIs from the children of a catalog element.
	 */
	static List<String> collectDatasetRefs(final Element catalog) {
		return streamChildren(catalog)
			.filter(DcatElements::isDatasetRef)
			.map(el -> el.getAttributeNS(RDF_NS, "resource"))
			.filter(ref -> !ref.isEmpty())
			.toList();
	}

	/**
	 * Streams the direct child elements of the given parent.
	 */
	static Stream<Element> streamChildren(final Element parent) {
		return streamElements(parent.getChildNodes());
	}

	/**
	 * Streams a {@link NodeList} as {@link Element}s, filtering out non-element nodes.
	 */
	static Stream<Element> streamElements(final NodeList nodes) {
		return IntStream.range(0, nodes.getLength())
			.mapToObj(nodes::item)
			.filter(Element.class::isInstance)
			.map(Element.class::cast);
	}

	/**
	 * Streams the attributes of a {@link NamedNodeMap}.
	 */
	static Stream<Node> streamAttributes(final NamedNodeMap attrs) {
		return IntStream.range(0, attrs.getLength())
			.mapToObj(attrs::item);
	}

	private static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
	private static final String DCTERMS_NS = "http://purl.org/dc/terms/";

	/**
	 * Returns {@code true} if the element is a {@code foaf:Agent}.
	 */
	static boolean isAgent(final Element element) {
		return hasName(element, FOAF_NS, "Agent");
	}

	/**
	 * Returns {@code true} if the element is a {@code dcterms:publisher} reference.
	 */
	static boolean isPublisherRef(final Element element) {
		return hasName(element, DCTERMS_NS, "publisher");
	}

	/**
	 * Returns the {@code rdf:about} attribute value, or empty if not present.
	 */
	static Optional<String> rdfAbout(final Element element) {
		return Optional.of(element.getAttributeNS(RDF_NS, "about"))
			.filter(s -> !s.isEmpty());
	}

	/**
	 * Returns the {@code rdf:resource} attribute value, or empty if not present.
	 */
	static Optional<String> rdfResource(final Element element) {
		return Optional.of(element.getAttributeNS(RDF_NS, "resource"))
			.filter(s -> !s.isEmpty());
	}

	private static boolean hasName(final Element element, final String namespace, final String localName) {
		return localName.equals(element.getLocalName()) && namespace.equals(element.getNamespaceURI());
	}
}
