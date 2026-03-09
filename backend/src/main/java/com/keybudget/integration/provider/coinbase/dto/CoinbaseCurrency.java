package com.keybudget.integration.provider.coinbase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Currency metadata for a Coinbase account.
 *
 * @param code short ticker or ISO code (e.g., "BTC", "ETH", "USD")
 * @param name full currency name (e.g., "Bitcoin", "Ethereum")
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinbaseCurrency(String code, String name) {}
