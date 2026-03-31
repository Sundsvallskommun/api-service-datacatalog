package se.sundsvall.datacatalog.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Catalog;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.ContactPoint;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.DataService;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Dataset;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Distribution;
import se.sundsvall.datacatalog.integration.dcat.DcatProperties.Publisher;

import static se.sundsvall.datacatalog.service.merger.DcatElements.DCAT_NS;
import static se.sundsvall.datacatalog.service.merger.DcatElements.RDF_NS;
import static se.sundsvall.datacatalog.service.merger.RdfParser.createDocument;
import static se.sundsvall.datacatalog.service.merger.RdfParser.serialize;

/**
 * Generates DCAT-AP-SE RDF/XML from configured catalog properties.
 */
@Component
public class DcatRdfGenerator {

	private static final String DCTERMS_NS = "http://purl.org/dc/terms/";
	private static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
	private static final String VCARD_NS = "http://www.w3.org/2006/vcard/ns#";
	private static final String ADMS_NS = "http://www.w3.org/ns/adms#";
	private static final String DCATAP_NS = "http://data.europa.eu/r5r/";
	private static final String XSD_NS = "http://www.w3.org/2001/XMLSchema#";

	private static final String PUBLISHER_TYPE_BASE = "http://purl.org/adms/publishertype/";
	private static final String DATA_THEME_BASE = "http://publications.europa.eu/resource/authority/data-theme/";
	private static final String ACCESS_RIGHT_BASE = "http://publications.europa.eu/resource/authority/access-right/";
	private static final String MEDIA_TYPE_BASE = "https://www.iana.org/assignments/media-types/";
	private static final String SCHEMA_NS = "http://schema.org/";
	private static final String DISTRIBUTION_STATUS_BASE = "http://publications.europa.eu/resource/authority/distribution-status/";
	private static final String AVAILABILITY_BASE = "http://data.europa.eu/r5r/availability/";
	private static final String LANGUAGE_SWE = "http://publications.europa.eu/resource/authority/language/SWE";
	private static final String LICENSE_CC0 = "http://creativecommons.org/publicdomain/zero/1.0/";

	/**
	 * Generates a complete DCAT-AP-SE RDF/XML catalog from the given configuration.
	 *
	 * @param  catalog the catalog definition
	 * @return         RDF/XML string
	 */
	public String generate(final Catalog catalog) {
		final var doc = createDocument();
		final var root = createRoot(doc);

		root.appendChild(buildPublisher(doc, catalog.publisher()));
		root.appendChild(buildCatalog(doc, catalog));

		Optional.ofNullable(catalog.datasets()).orElse(List.of()).forEach(dataset -> {
			root.appendChild(buildDataset(doc, dataset, catalog.publisher(), catalog.contactPoint()));
			Optional.ofNullable(dataset.distribution()).ifPresent(dist -> root.appendChild(buildDistribution(doc, dist)));
			Optional.ofNullable(dataset.dataService()).ifPresent(svc -> root.appendChild(buildDataService(doc, svc, dataset, catalog.contactPoint())));
		});

		return serialize(doc);
	}

	private static Element createRoot(final Document doc) {
		final var root = doc.createElementNS(RDF_NS, "rdf:RDF");
		root.setAttribute("xmlns:dcat", DCAT_NS);
		root.setAttribute("xmlns:dcterms", DCTERMS_NS);
		root.setAttribute("xmlns:foaf", FOAF_NS);
		root.setAttribute("xmlns:vcard", VCARD_NS);
		root.setAttribute("xmlns:rdf", RDF_NS);
		root.setAttribute("xmlns:adms", ADMS_NS);
		root.setAttribute("xmlns:dcatap", DCATAP_NS);
		root.setAttribute("xmlns:schema", SCHEMA_NS);
		root.setAttribute("xmlns:xsd", XSD_NS);
		doc.appendChild(root);
		return root;
	}

	private static Element buildPublisher(final Document doc, final Publisher publisher) {
		final var agent = element(doc, FOAF_NS, "foaf:Agent");
		agent.setAttributeNS(RDF_NS, "rdf:about", publisher.about());
		agent.appendChild(textElement(doc, FOAF_NS, "foaf:name", publisher.name()));
		agent.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:type", PUBLISHER_TYPE_BASE + publisher.type()));
		agent.appendChild(resourceElement(doc, FOAF_NS, "foaf:homepage", publisher.homepage()));
		// foaf:mbox must be rdf:resource, not text node
		agent.appendChild(resourceElement(doc, FOAF_NS, "foaf:mbox", "mailto:" + publisher.email()));
		return agent;
	}

	private static Element buildCatalog(final Document doc, final Catalog catalog) {
		final var el = element(doc, DCAT_NS, "dcat:Catalog");
		el.setAttributeNS(RDF_NS, "rdf:about", catalog.about());

		appendLang(doc, el, DCTERMS_NS, "dcterms:title", catalog.titleSv(), "sv");
		appendLang(doc, el, DCTERMS_NS, "dcterms:title", catalog.titleEn(), "en");
		appendLang(doc, el, DCTERMS_NS, "dcterms:description", catalog.descriptionSv(), "sv");
		appendLang(doc, el, DCTERMS_NS, "dcterms:description", catalog.descriptionEn(), "en");

		el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:publisher", catalog.publisher().about()));
		el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:license", LICENSE_CC0));
		el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:language", LANGUAGE_SWE));

		Optional.ofNullable(catalog.issued())
			.ifPresent(d -> el.appendChild(dateElement(doc, DCTERMS_NS, "dcterms:issued", d)));
		Optional.ofNullable(catalog.modified())
			.ifPresent(d -> el.appendChild(dateElement(doc, DCTERMS_NS, "dcterms:modified", d)));
		Optional.ofNullable(catalog.homepage())
			.ifPresent(h -> el.appendChild(resourceElement(doc, FOAF_NS, "foaf:homepage", h)));
		Optional.ofNullable(catalog.spatial())
			.ifPresent(s -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:spatial", s)));
		el.appendChild(resourceElement(doc, DCAT_NS, "dcat:themeTaxonomy", DATA_THEME_BASE.substring(0, DATA_THEME_BASE.length() - 1)));

		Optional.ofNullable(catalog.datasets()).orElse(List.of())
			.forEach(dataset -> el.appendChild(resourceElement(doc, DCAT_NS, "dcat:dataset", dataset.about())));

		return el;
	}

	private static Element buildDataset(final Document doc, final Dataset dataset, final Publisher publisher, final ContactPoint contactPoint) {
		final var el = element(doc, DCAT_NS, "dcat:Dataset");
		el.setAttributeNS(RDF_NS, "rdf:about", dataset.about());

		appendLang(doc, el, DCTERMS_NS, "dcterms:title", dataset.titleSv(), "sv");
		appendLang(doc, el, DCTERMS_NS, "dcterms:title", dataset.titleEn(), "en");
		appendLang(doc, el, DCTERMS_NS, "dcterms:description", dataset.descriptionSv(), "sv");
		appendLang(doc, el, DCTERMS_NS, "dcterms:description", dataset.descriptionEn(), "en");

		el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:publisher", publisher.about()));

		Optional.ofNullable(dataset.issued())
			.ifPresent(d -> el.appendChild(dateElement(doc, DCTERMS_NS, "dcterms:issued", d)));
		Optional.ofNullable(dataset.modified())
			.ifPresent(d -> el.appendChild(dateElement(doc, DCTERMS_NS, "dcterms:modified", d)));

		Optional.ofNullable(dataset.keywordsSv()).orElse(List.of())
			.forEach(kw -> appendLang(doc, el, DCAT_NS, "dcat:keyword", kw, "sv"));
		Optional.ofNullable(dataset.keywordsEn()).orElse(List.of())
			.forEach(kw -> appendLang(doc, el, DCAT_NS, "dcat:keyword", kw, "en"));

		Optional.ofNullable(dataset.theme())
			.ifPresent(theme -> el.appendChild(resourceElement(doc, DCAT_NS, "dcat:theme", DATA_THEME_BASE + theme)));
		Optional.ofNullable(dataset.accessRights())
			.ifPresent(ar -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:accessRights", ACCESS_RIGHT_BASE + ar)));
		Optional.ofNullable(dataset.spatial())
			.ifPresent(s -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:spatial", s)));
		Optional.ofNullable(dataset.conformsTo())
			.ifPresent(c -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:conformsTo", c)));

		buildTemporal(doc, dataset).ifPresent(el::appendChild);

		Optional.ofNullable(contactPoint).ifPresent(cp -> el.appendChild(buildContactPoint(doc, cp)));

		Optional.ofNullable(dataset.distribution())
			.ifPresent(dist -> el.appendChild(resourceElement(doc, DCAT_NS, "dcat:distribution", dist.about())));

		return el;
	}

	private static Optional<Element> buildTemporal(final Document doc, final Dataset dataset) {
		if (dataset.temporalStart() == null && dataset.temporalEnd() == null) {
			return Optional.empty();
		}

		final var temporal = element(doc, DCTERMS_NS, "dcterms:temporal");
		final var period = element(doc, DCTERMS_NS, "dcterms:PeriodOfTime");

		Optional.ofNullable(dataset.temporalStart())
			.ifPresent(s -> period.appendChild(dateElement(doc, DCAT_NS, "dcat:startDate", s)));
		Optional.ofNullable(dataset.temporalEnd())
			.ifPresent(e -> period.appendChild(dateElement(doc, DCAT_NS, "dcat:endDate", e)));

		temporal.appendChild(period);
		return Optional.of(temporal);
	}

	private static Element buildContactPoint(final Document doc, final ContactPoint cp) {
		final var wrapper = element(doc, DCAT_NS, "dcat:contactPoint");
		final var org = element(doc, VCARD_NS, "vcard:Organization");
		org.appendChild(textElement(doc, VCARD_NS, "vcard:fn", cp.name()));
		org.appendChild(resourceElement(doc, VCARD_NS, "vcard:hasEmail", "mailto:" + cp.email()));

		Optional.ofNullable(cp.phone()).ifPresent(phone -> {
			final var tel = element(doc, VCARD_NS, "vcard:hasTelephone");
			final var voice = element(doc, VCARD_NS, "vcard:Voice");
			voice.appendChild(resourceElement(doc, VCARD_NS, "vcard:hasValue", "tel:" + phone));
			tel.appendChild(voice);
			org.appendChild(tel);
		});

		if (cp.streetAddress() != null || cp.postalCode() != null || cp.locality() != null || cp.country() != null) {
			final var addrWrapper = element(doc, VCARD_NS, "vcard:hasAddress");
			final var addr = element(doc, VCARD_NS, "vcard:Address");
			Optional.ofNullable(cp.streetAddress()).ifPresent(s -> addr.appendChild(textElement(doc, VCARD_NS, "vcard:street-address", s)));
			Optional.ofNullable(cp.postalCode()).ifPresent(s -> addr.appendChild(textElement(doc, VCARD_NS, "vcard:postal-code", s)));
			Optional.ofNullable(cp.locality()).ifPresent(s -> addr.appendChild(textElement(doc, VCARD_NS, "vcard:locality", s)));
			Optional.ofNullable(cp.country()).ifPresent(s -> addr.appendChild(textElement(doc, VCARD_NS, "vcard:country-name", s)));
			addrWrapper.appendChild(addr);
			org.appendChild(addrWrapper);
		}

		wrapper.appendChild(org);
		return wrapper;
	}

	private static Element buildDistribution(final Document doc, final Distribution dist) {
		final var el = element(doc, DCAT_NS, "dcat:Distribution");
		el.setAttributeNS(RDF_NS, "rdf:about", dist.about());

		appendLang(doc, el, DCTERMS_NS, "dcterms:title", dist.titleSv(), "sv");
		appendLang(doc, el, DCTERMS_NS, "dcterms:title", dist.titleEn(), "en");
		appendLang(doc, el, DCTERMS_NS, "dcterms:description", dist.descriptionSv(), "sv");
		appendLang(doc, el, DCTERMS_NS, "dcterms:description", dist.descriptionEn(), "en");

		Optional.ofNullable(dist.accessUrl()).ifPresent(url -> el.appendChild(resourceElement(doc, DCAT_NS, "dcat:accessURL", url)));
		Optional.ofNullable(dist.mediaType()).ifPresent(mt -> el.appendChild(resourceElement(doc, DCAT_NS, "dcat:mediaType", MEDIA_TYPE_BASE + mt)));
		Optional.ofNullable(dist.mediaType()).ifPresent(mt -> el.appendChild(textElement(doc, DCTERMS_NS, "dcterms:format", mt)));
		Optional.ofNullable(dist.status()).ifPresent(s -> el.appendChild(resourceElement(doc, ADMS_NS, "adms:status", DISTRIBUTION_STATUS_BASE + s)));
		Optional.ofNullable(dist.availability()).ifPresent(a -> el.appendChild(resourceElement(doc, DCATAP_NS, "dcatap:availability", AVAILABILITY_BASE + a.toLowerCase())));
		Optional.ofNullable(dist.license()).ifPresent(lic -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:license", lic)));
		Optional.ofNullable(dist.conformsTo()).ifPresent(c -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:conformsTo", c)));
		Optional.ofNullable(dist.issued()).ifPresent(d -> el.appendChild(dateElement(doc, DCTERMS_NS, "dcterms:issued", d)));
		Optional.ofNullable(dist.modified()).ifPresent(d -> el.appendChild(dateElement(doc, DCTERMS_NS, "dcterms:modified", d)));
		Optional.ofNullable(dist.language()).ifPresent(l -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:language", l)));

		return el;
	}

	private static Element buildDataService(final Document doc, final DataService svc, final Dataset dataset, final ContactPoint contactPoint) {
		final var el = element(doc, DCAT_NS, "dcat:DataService");
		el.setAttributeNS(RDF_NS, "rdf:about", svc.about());

		appendLang(doc, el, DCTERMS_NS, "dcterms:title", svc.titleSv(), "sv");
		appendLang(doc, el, DCTERMS_NS, "dcterms:title", svc.titleEn(), "en");
		appendLang(doc, el, DCTERMS_NS, "dcterms:description", svc.descriptionSv(), "sv");

		Optional.ofNullable(svc.endpointUrl()).ifPresent(url -> el.appendChild(resourceElement(doc, DCAT_NS, "dcat:endpointURL", url)));
		Optional.ofNullable(svc.endpointDescription()).ifPresent(desc -> el.appendChild(resourceElement(doc, DCAT_NS, "dcat:endpointDescription", desc)));

		el.appendChild(resourceElement(doc, DCAT_NS, "dcat:servesDataset", dataset.about()));

		Optional.ofNullable(svc.license()).ifPresent(lic -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:license", lic)));
		Optional.ofNullable(svc.conformsTo()).ifPresent(c -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:conformsTo", c)));
		Optional.ofNullable(svc.accessRights()).ifPresent(ar -> el.appendChild(resourceElement(doc, DCTERMS_NS, "dcterms:accessRights", ACCESS_RIGHT_BASE + ar)));

		Optional.ofNullable(svc.keywordsSv()).orElse(List.of())
			.forEach(kw -> appendLang(doc, el, DCAT_NS, "dcat:keyword", kw, "sv"));

		Optional.ofNullable(contactPoint).ifPresent(cp -> el.appendChild(buildContactPoint(doc, cp)));

		return el;
	}

	private static void appendLang(final Document doc, final Element parent, final String ns, final String qualifiedName, final String value, final String lang) {
		Optional.ofNullable(value).ifPresent(v -> {
			final var el = element(doc, ns, qualifiedName);
			el.setAttribute("xml:lang", lang);
			el.setTextContent(v);
			parent.appendChild(el);
		});
	}

	private static Element dateElement(final Document doc, final String ns, final String qualifiedName, final String date) {
		final var el = element(doc, ns, qualifiedName);
		el.setAttributeNS(RDF_NS, "rdf:datatype", XSD_NS + "date");
		el.setTextContent(date);
		return el;
	}

	private static Element element(final Document doc, final String ns, final String qualifiedName) {
		return doc.createElementNS(ns, qualifiedName);
	}

	private static Element textElement(final Document doc, final String ns, final String qualifiedName, final String text) {
		final var el = element(doc, ns, qualifiedName);
		el.setTextContent(text);
		return el;
	}

	private static Element resourceElement(final Document doc, final String ns, final String qualifiedName, final String resource) {
		final var el = element(doc, ns, qualifiedName);
		el.setAttributeNS(RDF_NS, "rdf:resource", resource);
		return el;
	}
}
