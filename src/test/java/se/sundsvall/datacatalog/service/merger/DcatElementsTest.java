package se.sundsvall.datacatalog.service.merger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.datacatalog.service.merger.DcatElements.DCAT_NS;
import static se.sundsvall.datacatalog.service.merger.DcatElements.RDF_NS;

class DcatElementsTest {

	@Test
	void isCatalogReturnsTrueForCatalog() {
		final var doc = RdfParser.createDocument();
		final var element = doc.createElementNS(DCAT_NS, "dcat:Catalog");

		assertThat(DcatElements.isCatalog(element)).isTrue();
	}

	@Test
	void isCatalogReturnsFalseForDataset() {
		final var doc = RdfParser.createDocument();
		final var element = doc.createElementNS(DCAT_NS, "dcat:Dataset");

		assertThat(DcatElements.isCatalog(element)).isFalse();
	}

	@Test
	void isAgentReturnsTrueForAgent() {
		final var doc = RdfParser.createDocument();
		final var element = doc.createElementNS("http://xmlns.com/foaf/0.1/", "foaf:Agent");

		assertThat(DcatElements.isAgent(element)).isTrue();
	}

	@Test
	void elementKeyIncludesAbout() {
		final var doc = RdfParser.createDocument();
		final var element = doc.createElementNS(DCAT_NS, "dcat:Dataset");
		element.setAttributeNS(RDF_NS, "rdf:about", "http://example.com/dataset1");

		final var key = DcatElements.elementKey(element);

		assertThat(key).contains("Dataset").contains("http://example.com/dataset1");
	}

	@Test
	void rdfAboutReturnsValueWhenPresent() {
		final var doc = RdfParser.createDocument();
		final var element = doc.createElementNS(DCAT_NS, "dcat:Dataset");
		element.setAttributeNS(RDF_NS, "rdf:about", "http://example.com/ds");

		assertThat(DcatElements.rdfAbout(element)).contains("http://example.com/ds");
	}

	@Test
	void rdfAboutReturnsEmptyWhenMissing() {
		final var doc = RdfParser.createDocument();
		final var element = doc.createElementNS(DCAT_NS, "dcat:Dataset");

		assertThat(DcatElements.rdfAbout(element)).isEmpty();
	}

	@Test
	void collectDatasetRefsFromCatalog() {
		final var doc = RdfParser.createDocument();
		final var catalog = doc.createElementNS(DCAT_NS, "dcat:Catalog");
		final var ref = doc.createElementNS(DCAT_NS, "dcat:dataset");
		ref.setAttributeNS(RDF_NS, "rdf:resource", "http://example.com/dataset1");
		catalog.appendChild(ref);

		final var refs = DcatElements.collectDatasetRefs(catalog);

		assertThat(refs).containsExactly("http://example.com/dataset1");
	}

	@Test
	void streamElementsFiltersNonElements() {
		final var doc = RdfParser.createDocument();
		final var root = doc.createElementNS(RDF_NS, "rdf:RDF");
		doc.appendChild(root);
		root.appendChild(doc.createTextNode("text"));
		root.appendChild(doc.createElementNS(DCAT_NS, "dcat:Dataset"));
		root.appendChild(doc.createComment("comment"));

		final var elements = DcatElements.streamElements(root.getChildNodes()).toList();

		assertThat(elements).hasSize(1);
		assertThat(elements.getFirst().getLocalName()).isEqualTo("Dataset");
	}
}
