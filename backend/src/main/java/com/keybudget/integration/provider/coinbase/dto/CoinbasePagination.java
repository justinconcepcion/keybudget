package com.keybudget.integration.provider.coinbase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Pagination cursor returned alongside the Coinbase accounts list.
 * When {@link #nextUri()} is non-null, more pages are available and the caller
 * must issue a follow-up request against that URI path.
 *
 * @param nextUri relative URI for the next page (e.g., {@code /v2/accounts?starting_after=abc123}),
 *                or {@code null} when on the last page
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinbasePagination(@JsonProperty("next_uri") String nextUri) {}
