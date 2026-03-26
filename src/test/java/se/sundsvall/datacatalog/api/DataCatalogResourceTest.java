package se.sundsvall.datacatalog.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.datacatalog.Application;
import se.sundsvall.datacatalog.service.DataCatalogService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(classes = Application.class, webEnvironment = RANDOM_PORT)
@ActiveProfiles("junit")
@AutoConfigureWebTestClient
class DataCatalogResourceTest {

	@MockitoBean
	private DataCatalogService dataCatalogService;

	@Autowired
	private WebTestClient webTestClient;

	@Test
	void getDcatCatalog() {
		final var catalogXml = "<rdf:RDF>aggregated</rdf:RDF>";

		when(dataCatalogService.getAggregatedCatalog()).thenReturn(catalogXml);

		final var result = webTestClient.get()
			.uri("/datasets/dcat")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentType("application/rdf+xml")
			.expectBody(String.class)
			.returnResult()
			.getResponseBody();

		assertThat(result).isEqualTo(catalogXml);
		verify(dataCatalogService).getAggregatedCatalog();
		verifyNoMoreInteractions(dataCatalogService);
	}
}
