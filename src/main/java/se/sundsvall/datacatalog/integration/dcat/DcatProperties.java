package se.sundsvall.datacatalog.integration.dcat;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.dcat")
public record DcatProperties(
	Duration cacheDuration,
	List<RdfSource> rdfSources,
	Catalog catalog) {

	/**
	 * External source serving DCAT-AP-SE RDF/XML directly (e.g. Diwise).
	 */
	public record RdfSource(String name, String url) {

	}

	/**
	 * The DCAT catalog definition with publisher and datasets managed by this service.
	 */
	public record Catalog(
		String about,
		String titleSv,
		String titleEn,
		String descriptionSv,
		String descriptionEn,
		String issued,
		String modified,
		String homepage,
		String spatial,
		Publisher publisher,
		ContactPoint contactPoint,
		List<Dataset> datasets) {

	}

	public record Publisher(
		String about,
		String name,
		String type,
		String homepage,
		String email) {

	}

	public record ContactPoint(
		String name,
		String email,
		String phone,
		String streetAddress,
		String postalCode,
		String locality,
		String country) {

	}

	public record Dataset(
		String about,
		String titleSv,
		String titleEn,
		String descriptionSv,
		String descriptionEn,
		String issued,
		String modified,
		String spatial,
		String temporalStart,
		String temporalEnd,
		String conformsTo,
		List<String> keywordsSv,
		List<String> keywordsEn,
		String theme,
		String accessRights,
		Distribution distribution,
		DataService dataService) {

	}

	public record Distribution(
		String about,
		String titleSv,
		String titleEn,
		String descriptionSv,
		String descriptionEn,
		String accessUrl,
		String mediaType,
		String license,
		String status,
		String availability,
		String conformsTo,
		String issued,
		String modified,
		String language) {

	}

	public record DataService(
		String about,
		String titleSv,
		String titleEn,
		String descriptionSv,
		String endpointUrl,
		String endpointDescription,
		String license,
		String conformsTo,
		String accessRights,
		List<String> keywordsSv) {

	}
}
