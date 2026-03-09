package com.keybudget.integration.provider.coinbase.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for the Coinbase API v2 integration.
 * Properties are bound from the {@code integration.coinbase} prefix.
 *
 * <p>Defaults match the public Coinbase API; override per-environment if needed
 * (e.g., to point at a proxy in a test environment).
 */
@Configuration
@ConfigurationProperties(prefix = "integration.coinbase")
@Getter
@Setter
public class CoinbaseConfig {

    /**
     * Base URL for the Coinbase API. Defaults to the public endpoint.
     * Override with {@code integration.coinbase.api-url} in environment-specific properties.
     */
    private String apiUrl = "https://api.coinbase.com";

    /**
     * Coinbase API version date sent in the {@code CB-VERSION} header on every request.
     * Coinbase uses a date-based versioning scheme; bump this when adopting a newer API contract.
     */
    private String apiVersion = "2024-01-01";
}
