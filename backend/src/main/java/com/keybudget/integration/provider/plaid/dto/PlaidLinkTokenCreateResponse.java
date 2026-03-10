package com.keybudget.integration.provider.plaid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire-format response from the Plaid {@code POST /link/token/create} endpoint.
 *
 * @param linkToken  opaque token passed to the Plaid Link SDK on the frontend
 * @param expiration ISO-8601 expiration timestamp for the link token (typically 30 minutes)
 * @param requestId  Plaid request ID for support/debugging
 */
public record PlaidLinkTokenCreateResponse(
        @JsonProperty("link_token")  String linkToken,
        @JsonProperty("expiration")  String expiration,
        @JsonProperty("request_id") String requestId
) {}
