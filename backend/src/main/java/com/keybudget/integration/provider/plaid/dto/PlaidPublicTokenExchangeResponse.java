package com.keybudget.integration.provider.plaid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire-format response from the Plaid {@code POST /item/public_token/exchange} endpoint.
 *
 * @param accessToken permanent access token stored encrypted for future API calls
 * @param itemId      Plaid Item ID — the stable identifier for this user's institution link
 * @param requestId   Plaid request ID for support/debugging
 */
public record PlaidPublicTokenExchangeResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("item_id")      String itemId,
        @JsonProperty("request_id")   String requestId
) {}
