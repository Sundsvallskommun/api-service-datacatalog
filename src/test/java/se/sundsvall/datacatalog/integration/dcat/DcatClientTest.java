package se.sundsvall.datacatalog.integration.dcat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersUriSpec;
import org.springframework.web.client.RestClient.ResponseSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DcatClientTest {

	@Mock
	private RestClient restClient;

	@Mock
	private RequestHeadersUriSpec requestHeadersUriSpec;

	@Mock
	private ResponseSpec responseSpec;

	@Test
	void fetchAllCatalogs() {
		final var source1 = new DcatProperties.RdfSource("source1", "http://source1/dcat");
		final var source2 = new DcatProperties.RdfSource("source2", "http://source2/dcat");
		final var properties = new DcatProperties(Duration.ZERO, List.of(source1, source2), null);

		setupRestClientMock();
		when(responseSpec.body(String.class))
			.thenReturn("<rdf:RDF>1</rdf:RDF>")
			.thenReturn("<rdf:RDF>2</rdf:RDF>");

		final var client = new DcatClient(properties, restClient);
		final var result = client.fetchAllCatalogs();

		assertThat(result).hasSize(2);
		verify(restClient, times(2)).get();
	}

	@Test
	void fetchAllCatalogsContinuesOnFailure() {
		final var source1 = new DcatProperties.RdfSource("failing", "http://failing/dcat");
		final var source2 = new DcatProperties.RdfSource("working", "http://working/dcat");
		final var properties = new DcatProperties(Duration.ZERO, List.of(source1, source2), null);

		setupRestClientMock();
		when(requestHeadersUriSpec.retrieve())
			.thenThrow(new RuntimeException("Connection refused"))
			.thenReturn(responseSpec);
		when(responseSpec.body(String.class)).thenReturn("<rdf:RDF>ok</rdf:RDF>");

		final var client = new DcatClient(properties, restClient);
		final var result = client.fetchAllCatalogs();

		assertThat(result).hasSize(1);
	}

	@Test
	void fetchAllCatalogsReturnsCachedResult() {
		final var source = new DcatProperties.RdfSource("source", "http://source/dcat");
		final var properties = new DcatProperties(Duration.ofHours(1), List.of(source), null);

		setupRestClientMock();
		when(responseSpec.body(String.class)).thenReturn("<rdf:RDF>cached</rdf:RDF>");

		final var client = new DcatClient(properties, restClient);
		client.fetchAllCatalogs();
		client.fetchAllCatalogs();

		verify(restClient, times(1)).get();
	}

	@Test
	void fetchAllCatalogsReturnsEmptyWhenAllFail() {
		final var source = new DcatProperties.RdfSource("failing", "http://failing/dcat");
		final var properties = new DcatProperties(Duration.ZERO, List.of(source), null);

		setupRestClientMock();
		when(requestHeadersUriSpec.retrieve()).thenThrow(new RuntimeException("fail"));

		final var client = new DcatClient(properties, restClient);
		final var result = client.fetchAllCatalogs();

		assertThat(result).isEmpty();
	}

	@Test
	void fetchAllCatalogsWithEmptySources() {
		final var properties = new DcatProperties(Duration.ZERO, List.of(), null);

		final var client = new DcatClient(properties, restClient);
		final var result = client.fetchAllCatalogs();

		assertThat(result).isEmpty();
	}

	private void setupRestClientMock() {
		when(restClient.get()).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
		when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
	}
}
