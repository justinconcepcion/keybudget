package com.keybudget.integration.provider.plaid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wire-format request body for the Plaid {@code POST /item/public_token/exchange} endpoint.
 *
 * @param clientId    Plaid client ID
 * @param secret      Plaid secret for the target environment
 * @param publicToken the short-lived public token returned by Plaid Link on the frontend
 */
public record PlaidPublicTokenExchangeRequest(
        @JsonProperty("client_id")    String clientId,
        @JsonProperty("secret")       String secret,
        @JsonProperty("public_token") String publicToken
) {}
