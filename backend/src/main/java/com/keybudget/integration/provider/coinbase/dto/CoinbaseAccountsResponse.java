package com.keybudget.integration.provider.coinbase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Top-level response from the Coinbase {@code GET /v2/accounts} endpoint.
 *
 * @param data       list of Coinbase account objects
 * @param pagination pagination cursor; {@code nextUri} is non-null when more pages exist
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinbaseAccountsResponse(
        List<CoinbaseAccount> data,
        CoinbasePagination pagination
) {}
