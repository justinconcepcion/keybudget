package com.keybudget.integration.provider.plaid.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for the Plaid integration.
 * Properties are bound from the {@code integration.plaid} prefix.
 *
 * <p>The {@link #getBaseUrl()} method derives the Plaid API base URL from the
 * configured {@code env} value, so callers never need to hard-code environment-specific URLs.
 * Setting {@code integration.plaid.base-url} explicitly in properties overrides the derived URL.
 */
@Configuration
@ConfigurationProperties(prefix = "integration.plaid")
@Getter
@Setter
public class PlaidConfig {

    /**
     * Plaid client ID from the Plaid Dashboard.
     * Sourced from the {@code PLAID_CLIENT_ID} environment variable.
     */
    private String clientId;

    /**
     * Plaid secret key for the configured environment.
     * Sourced from the {@code PLAID_SECRET} environment variable.
     */
    private String secret;

    /**
     * Plaid environment: {@code sandbox}, {@code development}, or {@code production}.
     * Defaults to {@code sandbox}.
     */
    private String env = "sandbox";

    /**
     * Optional explicit base URL. When set, this value takes precedence over the URL
     * derived from {@link #env}. Set {@code integration.plaid.base-url} in properties
     * to pin to a specific URL without changing the env label.
     */
    private String baseUrl;

    /**
     * Returns the effective Plaid API base URL.
     * If {@code baseUrl} is set explicitly, that value is returned as-is.
     * Otherwise the URL is derived from {@code env}:
     * <ul>
     *   <li>{@code sandbox}     → {@code https://sandbox.plaid.com}</li>
     *   <li>{@code development} → {@code https://development.plaid.com}</li>
     *   <li>{@code production}  → {@code https://production.plaid.com}</li>
     * </ul>
     *
     * @return the base URL to use for all Plaid API calls
     * @throws IllegalStateException if {@code env} is not one of the recognised values
     */
    public String getEffectiveBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl;
        }
        return switch (env) {
            case "sandbox"     -> "https://sandbox.plaid.com";
            case "development" -> "https://development.plaid.com";
            case "production"  -> "https://production.plaid.com";
            default -> throw new IllegalStateException(
                    "Unknown Plaid env: '" + env + "'. Must be sandbox, development, or production.");
        };
    }
}
