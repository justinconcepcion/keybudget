package com.keybudget.integration.provider.plaid.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Wire-format request body for the Plaid {@code POST /link/token/create} endpoint.
 *
 * @param clientId     Plaid client ID
 * @param secret       Plaid secret for the target environment
 * @param clientName   application name shown in the Plaid Link UI
 * @param userId       user object identifying the end-user in Plaid
 * @param products     list of Plaid products to request (e.g., {@code investments}, {@code auth})
 * @param countryCodes ISO-3166-1 alpha-2 country codes (e.g., {@code ["US"]})
 * @param language     IETF language tag for the Plaid Link UI (e.g., {@code "en"})
 */
public record PlaidLinkTokenCreateRequest(
        @JsonProperty("client_id")   String clientId,
        @JsonProperty("secret")      String secret,
        @JsonProperty("client_name") String clientName,
        @JsonProperty("user")        PlaidUser userId,
        @JsonProperty("products")    List<String> products,
        @JsonProperty("country_codes") List<String> countryCodes,
        @JsonProperty("language")    String language
) {

    /**
     * Plaid user object embedded in the link-token create request.
     *
     * @param clientUserId a stable, unique identifier for the end-user in the calling application
     */
    public record PlaidUser(
            @JsonProperty("client_user_id") String clientUserId
    ) {}
}
