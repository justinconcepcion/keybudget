package com.keybudget.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
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

    private static final int MAX_IN_MEMORY_SIZE = 512 * 1024; // 512 KB

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
        return WebClient.builder()
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer.defaultCodecs()
                                .maxInMemorySize(MAX_IN_MEMORY_SIZE))
                        .build());
    }
}
