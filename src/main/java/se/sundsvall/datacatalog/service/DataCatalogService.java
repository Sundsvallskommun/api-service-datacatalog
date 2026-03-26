package se.sundsvall.datacatalog.service;

import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;
import se.sundsvall.datacatalog.integration.dcat.DcatClient;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties;
import se.sundsvall.datacatalog.service.merger.RdfMerger;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@Service
public class DataCatalogService {

	private final DcatClient dcatClient;
	private final DcatRdfGenerator dcatRdfGenerator;
	private final DcatProperties properties;
	private final RdfMerger rdfMerger;

	public DataCatalogService(final DcatClient dcatClient, final DcatRdfGenerator dcatRdfGenerator,
		final DcatProperties properties, final RdfMerger rdfMerger) {
		this.dcatClient = dcatClient;
		this.dcatRdfGenerator = dcatRdfGenerator;
		this.properties = properties;
		this.rdfMerger = rdfMerger;
	}

	public String getAggregatedCatalog() {
		final var catalogs = new ArrayList<>(dcatClient.fetchAllCatalogs());

		Optional.ofNullable(properties.catalog())
			.map(dcatRdfGenerator::generate)
			.ifPresent(catalogs::add);

		if (catalogs.isEmpty()) {
			throw Problem.valueOf(SERVICE_UNAVAILABLE, "No DCAT sources available");
		}

		final var canonicalPublisher = Optional.ofNullable(properties.catalog())
			.map(DcatProperties.Catalog::publisher)
			.map(DcatProperties.Publisher::about)
			.orElse(null);

		return rdfMerger.merge(catalogs, canonicalPublisher);
	}
}
