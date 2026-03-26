package se.sundsvall.datacatalog.service.merger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RdfParserTest {

	@Test
	void parseValidXml() {
		final var xml = "<?xml version=\"1.0\"?><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>";

		final var doc = RdfParser.parse(xml);

		assertThat(doc).isNotNull();
		assertThat(doc.getDocumentElement().getLocalName()).isEqualTo("RDF");
	}

	@Test
	void parseInvalidXmlThrows() {
		assertThatThrownBy(() -> RdfParser.parse("not xml"))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to parse RDF/XML");
	}

	@Test
	void createDocumentReturnsEmptyDocument() {
		final var doc = RdfParser.createDocument();

		assertThat(doc).isNotNull();
		assertThat(doc.getDocumentElement()).isNull();
	}

	@Test
	void serializeProducesXml() {
		final var doc = RdfParser.createDocument();
		final var root = doc.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:RDF");
		doc.appendChild(root);

		final var result = RdfParser.serialize(doc);

		assertThat(result)
			.contains("<?xml")
			.contains("rdf:RDF")
			.contains("UTF-8");
	}

	@Test
	void serializeWithChildElements() {
		final var doc = RdfParser.createDocument();
		final var root = doc.createElementNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf:RDF");
		doc.appendChild(root);
		final var child = doc.createElementNS("http://www.w3.org/ns/dcat#", "dcat:Dataset");
		child.setTextContent("test content");
		root.appendChild(child);

		final var result = RdfParser.serialize(doc);

		assertThat(result)
			.contains("dcat:Dataset")
			.contains("test content");
	}

	@Test
	void parseAndSerializeRoundTrip() {
		final var xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:Description rdf:about=\"http://example.com\"/></rdf:RDF>";

		final var doc = RdfParser.parse(xml);
		final var result = RdfParser.serialize(doc);

		assertThat(result)
			.contains("rdf:RDF")
			.contains("http://example.com");
	}

	@Test
	void parseNullThrows() {
		assertThatThrownBy(() -> RdfParser.parse(null))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to parse RDF/XML");
	}

	@Test
	void parseEmptyStringThrows() {
		assertThatThrownBy(() -> RdfParser.parse(""))
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Failed to parse RDF/XML");
	}

	@Test
	void serializeEmptyDocumentProducesXml() {
		final var doc = RdfParser.createDocument();

		final var result = RdfParser.serialize(doc);

		assertThat(result).contains("<?xml");
	}
}
