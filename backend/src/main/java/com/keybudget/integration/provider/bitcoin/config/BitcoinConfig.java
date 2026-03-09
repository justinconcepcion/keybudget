package com.keybudget.integration.provider.bitcoin.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Externalized configuration for the Bitcoin / Blockstream Esplora integration.
 * Properties are bound from the {@code integration.bitcoin} prefix.
 */
@Configuration
@ConfigurationProperties(prefix = "integration.bitcoin")
@Getter
@Setter
public class BitcoinConfig {

    /**
     * Base URL for the Blockstream Esplora API.
     * Defaults to the public Blockstream endpoint; override in environment-specific
     * properties for self-hosted Esplora deployments or Mempool.space.
     */
    private String blockstreamUrl = "https://blockstream.info/api";
}
