package com.keybudget.integration.provider.bitcoin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for the CoinGecko price API.
 * Properties are bound from the {@code integration.coingecko} prefix.
 *
 * <p>CoinGecko's public API is free with a rate limit of ~30 calls/minute.
 * No API key is required at this tier.
 */
@Configuration
@ConfigurationProperties(prefix = "integration.coingecko")
@Getter
@Setter
public class CoinGeckoConfig {

    /**
     * URL for the CoinGecko simple price endpoint.
     * Defaults to the public CoinGecko v3 endpoint.
     */
    private String priceUrl = "https://api.coingecko.com/api/v3/simple/price";
}
