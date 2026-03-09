package com.keybudget.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Application-level WebClient configuration.
 *
 * <p>Spring Boot auto-configures a {@link WebClient.Builder} prototype bean when
 * {@code spring-boot-starter-webflux} is on the classpath. This class makes the
 * configuration explicit and provides a single place to add shared defaults
 * (codecs, base headers, filters) if needed in the future.
 */
@Configuration
public class WebClientConfig {

    /**
     * Provides a pre-configured {@link WebClient.Builder} that provider implementations
     * inject and use to construct their own {@link WebClient} instances with provider-specific
     * base URLs. Using the builder (rather than a fully-built WebClient) keeps each
     * provider's base URL encapsulated within that provider.
     *
     * @return a Spring-managed {@link WebClient.Builder}
     */
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
