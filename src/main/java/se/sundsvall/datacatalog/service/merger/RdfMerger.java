package se.sundsvall.datacatalog.service.merger;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import static se.sundsvall.datacatalog.service.merger.DcatElements.DCAT_NS;
import static se.sundsvall.datacatalog.service.merger.DcatElements.RDF_NS;
import static se.sundsvall.datacatalog.service.merger.DcatElements.collectDatasetRefs;
import static se.sundsvall.datacatalog.service.merger.DcatElements.elementKey;
import static se.sundsvall.datacatalog.service.merger.DcatElements.isAgent;
import static se.sundsvall.datacatalog.service.merger.DcatElements.isCatalog;
import static se.sundsvall.datacatalog.service.merger.DcatElements.rdfAbout;
import static se.sundsvall.datacatalog.service.merger.DcatElements.rdfResource;
import static se.sundsvall.datacatalog.service.merger.DcatElements.streamChildren;
import static se.sundsvall.datacatalog.service.merger.DcatElements.streamElements;
import static se.sundsvall.datacatalog.service.merger.RdfParser.createDocument;
import static se.sundsvall.datacatalog.service.merger.RdfParser.serialize;

/**
 * Merges multiple DCAT-AP-SE RDF/XML catalogs into a single catalog.
 * <p>
 * Collects all dataset references, deduplicates top-level elements by rdf:about, and produces a unified document with
 * one merged dcat:Catalog.
 */
@Component
public class RdfMerger {

	/**
	 * Creates the output rdf:RDF root element with namespace declarations merged from all source documents.
	 */
	private static Element createRootWithMergedNamespaces(final Document outputDoc, final List<Document> documents) {
		final var outputRoot = (Element) outputDoc.importNode(documents.getFirst().getDocumentElement(), false);
		outputDoc.appendChild(outputRoot);

		documents.stream()
			.map(doc -> doc.getDocumentElement().getAttributes())
			.flatMap(DcatElements::streamAttributes)
			.filter(attr -> !outputRoot.hasAttribute(attr.getNodeName()))
			.forEach(attr -> outputRoot.setAttribute(attr.getNodeName(), attr.getNodeValue()));

		return outputRoot;
	}

	/**
	 * Builds a single merged dcat:Catalog element using the first catalog found as template, replacing its dataset
	 * references with the complete set from all sources.
	 */
	private static Optional<Element> buildMergedCatalog(final List<Document> documents, final Document outputDoc,
		final LinkedHashSet<String> datasetRefs) {

		return documents.stream()
			.flatMap(doc -> streamElements(doc.getDocumentElement().getChildNodes()))
			.filter(DcatElements::isCatalog)
			.findFirst()
			.map(catalog -> {
				final var merged = (Element) outputDoc.importNode(catalog, true);

				streamChildren(merged)
					.filter(DcatElements::isDatasetRef)
					.toList()
					.forEach(merged::removeChild);

				datasetRefs.forEach(ref -> {
					final var element = outputDoc.createElementNS(DCAT_NS, "dcat:dataset");
					element.setAttributeNS(RDF_NS, "rdf:resource", ref);
					merged.appendChild(element);
				});

				return merged;
			});
	}

	/**
	 * Returns {@code true} if the agent element's {@code rdf:about} matches the canonical publisher URI.
	 */
	private static boolean matchesCanonicalPublisher(final Element agent, final String canonicalPublisher) {
		return rdfAbout(agent)
			.map(canonicalPublisher::equals)
			.orElse(false);
	}

	/**
	 * Rewrites all {@code dcterms:publisher} resource references within the element to the canonical URI. No-op if
	 * {@code canonicalPublisher} is {@code null}.
	 */
	private static void normalizePublisherRefs(final Element element, final String canonicalPublisher) {
		Optional.ofNullable(canonicalPublisher).ifPresent(publisher -> streamChildren(element)
			.filter(DcatElements::isPublisherRef)
			.filter(pub -> rdfResource(pub).isPresent())
			.forEach(pub -> pub.setAttributeNS(RDF_NS, "rdf:resource", publisher)));
	}

	/**
	 * Merges one or more DCAT-AP-SE RDF/XML catalog strings into a single catalog.
	 * <p>
	 * If only one catalog is provided it is returned as-is. For multiple catalogs, the merge deduplicates top-level
	 * elements by {@code rdf:about} and produces a unified {@code dcat:Catalog} referencing all datasets from all sources.
	 *
	 * @param  catalogs           list of RDF/XML strings, each containing a complete DCAT catalog
	 * @param  canonicalPublisher the canonical publisher URI — all {@code dcterms:publisher} references are rewritten to
	 *                            this URI, and only the {@code foaf:Agent} with this {@code rdf:about} is kept
	 * @return                    merged RDF/XML string
	 */
	public String merge(final List<String> catalogs, final String canonicalPublisher) {
		if (catalogs.size() == 1) {
			return catalogs.getFirst();
		}

		final var documents = catalogs.stream()
			.map(RdfParser::parse)
			.toList();

		final var outputDoc = createDocument();
		final var outputRoot = createRootWithMergedNamespaces(outputDoc, documents);

		final var datasetRefs = new LinkedHashSet<String>();
		final var elements = new LinkedHashMap<String, Node>();

		documents.forEach(doc -> streamElements(doc.getDocumentElement().getChildNodes()).forEach(element -> {
			if (isCatalog(element)) {
				datasetRefs.addAll(collectDatasetRefs(element));
			} else if (canonicalPublisher != null && isAgent(element) && !matchesCanonicalPublisher(element, canonicalPublisher)) {
				// Skip non-canonical publisher agents
			} else {
				elements.putIfAbsent(elementKey(element), outputDoc.importNode(element, true));
			}
		}));

		buildMergedCatalog(documents, outputDoc, datasetRefs)
			.ifPresent(catalog -> {
				normalizePublisherRefs(catalog, canonicalPublisher);
				outputRoot.appendChild(catalog);
			});

		elements.values().forEach(node -> {
			if (node instanceof Element el) {
				normalizePublisherRefs(el, canonicalPublisher);
			}
			outputRoot.appendChild(node);
		});

		return serialize(outputDoc);
	}
}
