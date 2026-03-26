package se.sundsvall.datacatalog.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.sundsvall.datacatalog.service.DataCatalogService;

@RestController
@RequestMapping("/datasets")
@Tag(name = "DCAT-AP-SE")
class DataCatalogResource {

	static final MediaType APPLICATION_RDF_XML = MediaType.parseMediaType("application/rdf+xml");

	private final DataCatalogService dataCatalogService;

	DataCatalogResource(final DataCatalogService dataCatalogService) {
		this.dataCatalogService = dataCatalogService;
	}

	@GetMapping(value = "/dcat")
	@Operation(summary = "Get aggregated DCAT-AP-SE metadata catalog", responses = {
		@ApiResponse(responseCode = "200", description = "Successful Operation", useReturnTypeSchema = true)
	})
	ResponseEntity<String> getDcatCatalog() {
		return ResponseEntity.ok()
			.contentType(APPLICATION_RDF_XML)
			.body(dataCatalogService.getAggregatedCatalog());
	}
}
