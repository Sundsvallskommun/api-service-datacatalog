package se.sundsvall.datacatalog.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.datacatalog.integration.dcat.DcatClient;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Catalog;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Publisher;
import se.sundsvall.datacatalog.service.merger.RdfMerger;
import se.sundsvall.dept44.problem.Problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataCatalogServiceTest {

	@Mock
	private DcatClient dcatClient;

	@Mock
	private DcatRdfGenerator dcatRdfGenerator;

	@Mock
	private DcatProperties properties;

	@Mock
	private RdfMerger rdfMerger;

	@InjectMocks
	private DataCatalogService service;

	@Test
	void getAggregatedCatalogWithExternalAndGenerated() {
		final var externalRdf = "<rdf:RDF>external</rdf:RDF>";
		final var generatedRdf = "<rdf:RDF>generated</rdf:RDF>";
		final var mergedRdf = "<rdf:RDF>merged</rdf:RDF>";
		final var publisherUri = "http://example.com/publisher";
		final var publisher = new Publisher(publisherUri, "Name", "LocalAuthority", "http://example.com", "test@example.com");
		final var catalog = new Catalog("http://example.com/catalog", "Titel", "Title", "Beskrivning", "Description",
			"2024-01-01", null, null, null, publisher, null, null);

		when(dcatClient.fetchAllCatalogs()).thenReturn(List.of(externalRdf));
		when(properties.catalog()).thenReturn(catalog);
		when(dcatRdfGenerator.generate(catalog)).thenReturn(generatedRdf);
		when(rdfMerger.merge(List.of(externalRdf, generatedRdf), publisherUri)).thenReturn(mergedRdf);

		final var result = service.getAggregatedCatalog();

		assertThat(result).isEqualTo(mergedRdf);
		verify(dcatClient).fetchAllCatalogs();
		verify(dcatRdfGenerator).generate(catalog);
		verify(rdfMerger).merge(List.of(externalRdf, generatedRdf), publisherUri);
	}

	@Test
	void getAggregatedCatalogWithOnlyExternal() {
		final var externalRdf = "<rdf:RDF>external</rdf:RDF>";
		final var mergedRdf = "<rdf:RDF>merged</rdf:RDF>";

		when(dcatClient.fetchAllCatalogs()).thenReturn(List.of(externalRdf));
		when(properties.catalog()).thenReturn(null);
		when(rdfMerger.merge(List.of(externalRdf), null)).thenReturn(mergedRdf);

		final var result = service.getAggregatedCatalog();

		assertThat(result).isEqualTo(mergedRdf);
		verifyNoInteractions(dcatRdfGenerator);
	}

	@Test
	void getAggregatedCatalogThrowsWhenEmpty() {
		when(dcatClient.fetchAllCatalogs()).thenReturn(List.of());
		when(properties.catalog()).thenReturn(null);

		assertThatThrownBy(service::getAggregatedCatalog)
			.isInstanceOf(Problem.class)
			.hasMessageContaining("No DCAT sources available");
		verify(rdfMerger, never()).merge(any(), any());
	}
}
