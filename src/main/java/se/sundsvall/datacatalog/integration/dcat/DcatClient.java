package se.sundsvall.datacatalog.integration.dcat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Fetches DCAT-AP-SE RDF/XML catalogs from configured external sources.
 * <p>
 * Results are cached for the duration configured in {@link DcatProperties#cacheDuration()}.
 */
@Component
public class DcatClient {

	private static final Logger LOG = LoggerFactory.getLogger(DcatClient.class);

	private final DcatProperties properties;
	private final RestClient restClient;

	private List<String> cachedCatalogs;
	private Instant cacheExpiry = Instant.MIN;

	DcatClient(final DcatProperties properties, final RestClient dcatRestClient) {
		this.properties = properties;
		this.restClient = dcatRestClient;
	}

	/**
	 * Fetches RDF/XML from all configured sources, returning successfully fetched catalogs.
	 *
	 * @return list of RDF/XML strings
	 */
	public List<String> fetchAllCatalogs() {
		if (Instant.now().isBefore(cacheExpiry) && cachedCatalogs != null) {
			return cachedCatalogs;
		}

		final var results = properties.rdfSources().stream()
			.map(this::fetchCatalog)
			.flatMap(Optional::stream)
			.toList();

		cachedCatalogs = results;
		cacheExpiry = Instant.now().plus(properties.cacheDuration());
		return cachedCatalogs;
	}

	private Optional<String> fetchCatalog(final DcatProperties.RdfSource source) {
		try {
			LOG.info("Fetching DCAT catalog from source: {} ({})", source.name(), source.url());
			final var body = restClient.get()
				.uri(source.url())
				.retrieve()
				.body(String.class);
			LOG.info("Successfully fetched DCAT catalog from source: {}", source.name());
			return Optional.ofNullable(body);
		} catch (final Exception e) {
			LOG.warn("Failed to fetch DCAT catalog from source: {} ({})", source.name(), source.url(), e);
			return Optional.empty();
		}
	}
}
