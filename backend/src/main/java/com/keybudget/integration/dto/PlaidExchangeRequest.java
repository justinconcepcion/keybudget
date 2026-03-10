package com.keybudget.integration.dto;

import com.keybudget.integration.ProviderType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Request payload for {@code POST /api/v1/integrations/plaid/exchange}.
 *
 * <p>After the user completes the Plaid Link flow, the frontend receives a short-lived
 * public token and must immediately POST it here (along with which provider was connected)
 * so the backend can exchange it for a permanent access token and complete the integration.
 *
 * @param publicToken the short-lived public token returned by the Plaid Link SDK
 * @param provider    the provider that was connected via Plaid Link (e.g., {@code M1_FINANCE})
 */
public record PlaidExchangeRequest(

        @NotBlank(message = "publicToken must not be blank")
        @Pattern(regexp = "^public-(sandbox|development|production)-[a-f0-9\\-]{36}$",
                message = "publicToken must be a valid Plaid public token")
        String publicToken,

        @NotNull(message = "provider is required")
        ProviderType provider
) {}
