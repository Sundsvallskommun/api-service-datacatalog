package se.sundsvall.datacatalog.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Catalog;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.ContactPoint;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.DataService;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Dataset;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Distribution;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Publisher;

import static org.assertj.core.api.Assertions.assertThat;

class DcatRdfGeneratorTest {

	private final DcatRdfGenerator generator = new DcatRdfGenerator();

	private static final Publisher PUBLISHER = new Publisher(
		"http://example.com/publisher", "Test Kommun", "LocalAuthority", "http://example.com", "info@example.com");

	private static final ContactPoint CONTACT = new ContactPoint(
		"IT-avdelningen", "it@example.com", "+46123456", "Gatan 1", "12345", "Teststad", "Sverige");

	private static final Distribution DISTRIBUTION = new Distribution(
		"http://example.com/dist", "Dist SV", "Dist EN", "Beskrivning", "Description",
		"http://example.com/api/data", "application/json", "http://creativecommons.org/publicdomain/zero/1.0/",
		"COMPLETED", "stable");

	private static final DataService DATA_SERVICE = new DataService(
		"http://example.com/service", "Service SV", "Service EN", "Beskrivning",
		"http://example.com/api", "http://example.com/api-docs",
		"http://creativecommons.org/publicdomain/zero/1.0/", "https://spec.openapis.org/oas/v3.1",
		"PUBLIC", List.of("keyword1"));

	private static final Dataset DATASET = new Dataset(
		"http://example.com/dataset", "Dataset SV", "Dataset EN", "Beskrivning", "Description",
		"2024-01-01", null, "http://sws.geonames.org/123", "2024-01-01", null,
		"http://example.com/api-docs", List.of("nyckelord"), List.of("keyword"),
		"ENVI", "PUBLIC", DISTRIBUTION, DATA_SERVICE);

	private static final Catalog CATALOG = new Catalog(
		"http://example.com/catalog", "Katalog SV", "Catalog EN", "Beskrivning", "Description",
		"2024-01-01", "2024-06-01", "http://example.com", "http://sws.geonames.org/123",
		PUBLISHER, CONTACT, List.of(DATASET));

	@Test
	void generateContainsCatalog() {
		final var result = generator.generate(CATALOG);

		assertThat(result)
			.contains("dcat:Catalog")
			.contains("Katalog SV")
			.contains("Catalog EN")
			.contains("http://example.com/catalog");
	}

	@Test
	void generateContainsPublisher() {
		final var result = generator.generate(CATALOG);

		assertThat(result)
			.contains("foaf:Agent")
			.contains("Test Kommun")
			.contains("LocalAuthority")
			.contains("mailto:info@example.com")
			.contains("rdf:resource");
	}

	@Test
	void generateContainsDataset() {
		final var result = generator.generate(CATALOG);

		assertThat(result)
			.contains("dcat:Dataset")
			.contains("Dataset SV")
			.contains("Dataset EN")
			.contains("nyckelord")
			.contains("keyword")
			.contains("data-theme/ENVI")
			.contains("access-right/PUBLIC");
	}

	@Test
	void generateContainsDistribution() {
		final var result = generator.generate(CATALOG);

		assertThat(result)
			.contains("dcat:Distribution")
			.contains("Dist SV")
			.contains("http://example.com/api/data")
			.contains("application/json")
			.contains("distribution-status/COMPLETED")
			.contains("availability/stable");
	}

	@Test
	void generateContainsDataService() {
		final var result = generator.generate(CATALOG);

		assertThat(result)
			.contains("dcat:DataService")
			.contains("Service SV")
			.contains("http://example.com/api")
			.contains("http://example.com/api-docs")
			.contains("keyword1");
	}

	@Test
	void generateContainsContactPoint() {
		final var result = generator.generate(CATALOG);

		assertThat(result)
			.contains("vcard:Organization")
			.contains("IT-avdelningen")
			.contains("mailto:it@example.com")
			.contains("tel:+46123456")
			.contains("Gatan 1")
			.contains("12345")
			.contains("Teststad")
			.contains("Sverige");
	}

	@Test
	void generateContainsTemporal() {
		final var result = generator.generate(CATALOG);

		assertThat(result)
			.contains("dcterms:PeriodOfTime")
			.contains("dcat:startDate")
			.contains("2024-01-01");
	}

	@Test
	void generateContainsSpatial() {
		final var result = generator.generate(CATALOG);

		assertThat(result)
			.contains("dcterms:spatial")
			.contains("http://sws.geonames.org/123");
	}

	@Test
	void generateContainsMboxAsResource() {
		final var result = generator.generate(CATALOG);

		// foaf:mbox must be rdf:resource, not text node
		assertThat(result).contains("foaf:mbox rdf:resource=\"mailto:info@example.com\"");
	}

	@Test
	void generateWithMinimalCatalog() {
		final var minimalCatalog = new Catalog(
			"http://example.com/catalog", "Titel", null, "Beskrivning", null,
			null, null, null, null, PUBLISHER, null, null);

		final var result = generator.generate(minimalCatalog);

		assertThat(result)
			.contains("dcat:Catalog")
			.contains("foaf:Agent")
			.contains("Titel")
			.doesNotContain("dcat:Dataset")
			.doesNotContain("dcat:Distribution");
	}

	@Test
	void generateWithNullOptionalFields() {
		final var dataset = new Dataset(
			"http://example.com/ds", "Titel", null, "Beskrivning", null,
			null, null, null, null, null, null, null, null, null, null, null, null);

		final var catalog = new Catalog(
			"http://example.com/cat", "Katalog", null, "Beskrivning", null,
			null, null, null, null, PUBLISHER, null, List.of(dataset));

		final var result = generator.generate(catalog);

		assertThat(result)
			.contains("dcat:Catalog")
			.contains("dcat:Dataset")
			.doesNotContain("dcterms:issued")
			.doesNotContain("dcterms:modified")
			.doesNotContain("dcterms:spatial")
			.doesNotContain("dcterms:temporal")
			.doesNotContain("dcterms:conformsTo")
			.doesNotContain("dcat:theme rdf:resource")
			.doesNotContain("dcterms:accessRights")
			.doesNotContain("dcat:Distribution")
			.doesNotContain("dcat:DataService")
			.doesNotContain("vcard:Organization");
	}

	@Test
	void generateWithNullDistributionFields() {
		final var dist = new Distribution(
			"http://example.com/dist", "Titel", null, null, null,
			null, null, null, null, null);

		final var dataset = new Dataset(
			"http://example.com/ds", "DS", null, "Beskrivning", null,
			null, null, null, null, null, null, null, null, null, null, dist, null);

		final var catalog = new Catalog(
			"http://example.com/cat", "Kat", null, "Beskrivning", null,
			null, null, null, null, PUBLISHER, null, List.of(dataset));

		final var result = generator.generate(catalog);

		assertThat(result)
			.contains("dcat:Distribution")
			.doesNotContain("dcat:accessURL")
			.doesNotContain("dcat:mediaType")
			.doesNotContain("adms:status")
			.doesNotContain("dcatap:availability");
	}

	@Test
	void generateWithNullDataServiceFields() {
		final var svc = new DataService(
			"http://example.com/svc", "Svc", null, null,
			null, null, null, null, null, null);

		final var dataset = new Dataset(
			"http://example.com/ds", "DS", null, "Beskrivning", null,
			null, null, null, null, null, null, null, null, null, null, null, svc);

		final var catalog = new Catalog(
			"http://example.com/cat", "Kat", null, "Beskrivning", null,
			null, null, null, null, PUBLISHER, null, List.of(dataset));

		final var result = generator.generate(catalog);

		assertThat(result)
			.contains("dcat:DataService")
			.doesNotContain("dcat:endpointURL")
			.doesNotContain("dcat:endpointDescription")
			.doesNotContain("dcterms:conformsTo")
			.doesNotContain("dcterms:accessRights");
	}

	@Test
	void generateContactPointWithoutPhoneAndAddress() {
		final var cp = new ContactPoint("Namn", "email@test.se", null, null, null, null, null);

		final var dataset = new Dataset(
			"http://example.com/ds", "DS", null, "Beskrivning", null,
			null, null, null, null, null, null, null, null, null, null, null, null);

		final var catalog = new Catalog(
			"http://example.com/cat", "Kat", null, "Beskrivning", null,
			null, null, null, null, PUBLISHER, cp, List.of(dataset));

		final var result = generator.generate(catalog);

		assertThat(result)
			.contains("vcard:fn")
			.contains("vcard:hasEmail")
			.doesNotContain("vcard:hasTelephone")
			.doesNotContain("vcard:hasAddress");
	}

	@Test
	void generateWithTemporalEndDate() {
		final var dataset = new Dataset(
			"http://example.com/ds", "DS", null, "Beskrivning", null,
			null, null, null, "2024-01-01", "2024-12-31", null, null, null, null, null, null, null);

		final var catalog = new Catalog(
			"http://example.com/cat", "Kat", null, "Beskrivning", null,
			null, null, null, null, PUBLISHER, null, List.of(dataset));

		final var result = generator.generate(catalog);

		assertThat(result)
			.contains("dcat:startDate")
			.contains("dcat:endDate")
			.contains("2024-01-01")
			.contains("2024-12-31");
	}
}
