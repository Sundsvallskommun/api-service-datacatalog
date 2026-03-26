# DataCatalog

DCAT-AP-SE metadata aggregator for Sundsvalls kommun. Collects and merges DCAT catalogs from multiple sources into a single catalog for harvesting by [Sveriges Dataportal](https://dataportal.se).

## What it does

- Fetches DCAT-AP-SE RDF/XML from configured external sources (e.g. Diwise)
- Generates DCAT metadata for internally managed datasets (e.g. garbage schedules)
- Merges everything into a single `dcat:Catalog` with deduplicated publishers
- Serves the result at `GET /datasets/dcat` (`application/rdf+xml`)

## Getting Started

### Prerequisites

- **Java 25 or higher**
- **Maven**
- **Git**

### Installation

1. **Clone the repository:**

   ```bash
   git clone https://github.com/Sundsvallskommun/api-service-datacatalog.git
   cd api-service-datacatalog
   ```
2. **Build and run:**

   ```bash
   mvn spring-boot:run
   ```
3. **Verify:**

   ```bash
   curl http://localhost:8080/datasets/dcat
   ```

## Configuration

All DCAT metadata is configured in `application.yml` under `integration.dcat`.

### External RDF sources

Fetch existing DCAT catalogs from external services:

```yaml
integration:
  dcat:
    cache-duration: PT1H
    rdf-sources:
      - name: diwise
        url: https://sundsvall.diwise.io/api/opendata/datasets/dcat
```

### Managed datasets

Define datasets, distributions, and data services directly in config:

```yaml
catalog:
  about: https://api.sundsvall.se/datacatalog/datasets/dcat#catalog
  title-sv: Sundsvalls kommuns öppna data
  publisher:
    about: http://dataportal.se/organisation/SE2120002411
    name: Sundsvalls kommun
  datasets:
    - about: https://api.sundsvall.se/garbage/datasets/dcat#dataset
      title-sv: Sopschema Sundsvall
      # ...
```

See `application.yml` for the full configuration structure.

### Adding a new dataset

1. Add a new entry under `catalog.datasets` in `application.yml`
2. Include `distribution` and optionally `data-service` sub-objects
3. Restart the service

No code changes needed.

## API Documentation

- **Swagger UI:** [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

## Dependencies

- **Diwise** — External DCAT catalog source for Sundsvalls kommun
  - **URL:** `https://sundsvall.diwise.io/api/opendata/datasets/dcat`

## Contributing

Contributions are welcome! Please see [CONTRIBUTING.md](https://github.com/Sundsvallskommun/.github/blob/main/.github/CONTRIBUTING.md) for guidelines.

## License

This project is licensed under the [MIT License](LICENSE).

## Code status

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-datacatalog&metric=alert_status)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-datacatalog)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-datacatalog&metric=reliability_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-datacatalog)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-datacatalog&metric=security_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-datacatalog)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-datacatalog&metric=sqale_rating)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-datacatalog)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-datacatalog&metric=vulnerabilities)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-datacatalog)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=Sundsvallskommun_api-service-datacatalog&metric=bugs)](https://sonarcloud.io/summary/overall?id=Sundsvallskommun_api-service-datacatalog)

---

© 2026 Sundsvalls kommun
