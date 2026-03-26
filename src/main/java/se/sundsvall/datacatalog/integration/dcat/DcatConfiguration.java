package se.sundsvall.datacatalog.integration.dcat;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(DcatProperties.class)
class DcatConfiguration {

	@Bean
	RestClient dcatRestClient(final RestClient.Builder builder) {
		return builder.build();
	}
}
