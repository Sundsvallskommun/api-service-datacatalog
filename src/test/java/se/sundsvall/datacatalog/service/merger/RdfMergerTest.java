package se.sundsvall.datacatalog.service.merger;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RdfMergerTest {

	private final RdfMerger merger = new RdfMerger();

	private static final String CATALOG_A = """
		<?xml version="1.0" encoding="UTF-8"?>
		<rdf:RDF xmlns:dcat="http://www.w3.org/ns/dcat#"
		         xmlns:dcterms="http://purl.org/dc/terms/"
		         xmlns:foaf="http://xmlns.com/foaf/0.1/"
		         xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
		  <foaf:Agent rdf:about="http://example.com/publisherA">
		    <foaf:name>Publisher A</foaf:name>
		  </foaf:Agent>
		  <dcat:Catalog rdf:about="http://example.com/catalogA">
		    <dcterms:title xml:lang="sv">Katalog A</dcterms:title>
		    <dcterms:publisher rdf:resource="http://example.com/publisherA"/>
		    <dcat:dataset rdf:resource="http://example.com/datasetA"/>
		  </dcat:Catalog>
		  <dcat:Dataset rdf:about="http://example.com/datasetA">
		    <dcterms:title xml:lang="sv">Dataset A</dcterms:title>
		    <dcterms:publisher rdf:resource="http://example.com/publisherA"/>
		  </dcat:Dataset>
		</rdf:RDF>
		""";

	private static final String CATALOG_B = """
		<?xml version="1.0" encoding="UTF-8"?>
		<rdf:RDF xmlns:dcat="http://www.w3.org/ns/dcat#"
		         xmlns:dcterms="http://purl.org/dc/terms/"
		         xmlns:foaf="http://xmlns.com/foaf/0.1/"
		         xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
		         xmlns:adms="http://www.w3.org/ns/adms#">
		  <foaf:Agent rdf:about="http://example.com/canonical">
		    <foaf:name>Canonical Publisher</foaf:name>
		  </foaf:Agent>
		  <dcat:Catalog rdf:about="http://example.com/catalogB">
		    <dcterms:title xml:lang="sv">Katalog B</dcterms:title>
		    <dcterms:publisher rdf:resource="http://example.com/canonical"/>
		    <dcat:dataset rdf:resource="http://example.com/datasetB"/>
		  </dcat:Catalog>
		  <dcat:Dataset rdf:about="http://example.com/datasetB">
		    <dcterms:title xml:lang="sv">Dataset B</dcterms:title>
		    <dcterms:publisher rdf:resource="http://example.com/canonical"/>
		  </dcat:Dataset>
		</rdf:RDF>
		""";

	@Test
	void mergeSingleCatalogReturnsAsIs() {
		final var result = merger.merge(List.of(CATALOG_A), null);

		assertThat(result).isEqualTo(CATALOG_A);
	}

	@Test
	void mergeCombinesDatasets() {
		final var result = merger.merge(List.of(CATALOG_A, CATALOG_B), null);

		assertThat(result)
			.contains("Dataset A")
			.contains("Dataset B")
			.contains("http://example.com/datasetA")
			.contains("http://example.com/datasetB");
	}

	@Test
	void mergeMergesNamespaces() {
		final var result = merger.merge(List.of(CATALOG_A, CATALOG_B), null);

		assertThat(result).contains("xmlns:adms");
	}

	@Test
	void mergeDeduplicatesByRdfAbout() {
		final var result = merger.merge(List.of(CATALOG_A, CATALOG_A), null);

		// Dataset A should appear only once
		final var count = result.split("http://example.com/datasetA").length - 1;
		// Once in catalog ref + once as Dataset element = 2
		assertThat(count).isEqualTo(2);
	}

	@Test
	void mergeWithCanonicalPublisherFiltersAgents() {
		final var canonical = "http://example.com/canonical";
		final var result = merger.merge(List.of(CATALOG_A, CATALOG_B), canonical);

		assertThat(result)
			.contains("Canonical Publisher")
			.doesNotContain("Publisher A");
	}

	@Test
	void mergeWithCanonicalPublisherNormalizesRefs() {
		final var canonical = "http://example.com/canonical";
		final var result = merger.merge(List.of(CATALOG_A, CATALOG_B), canonical);

		// All publisher refs should point to canonical
		assertThat(result).doesNotContain("rdf:resource=\"http://example.com/publisherA\"");
	}
}
