package com.keybudget.integration.dto;

/**
 * API response DTO returned by {@code POST /api/v1/integrations/plaid/link-token}.
 *
 * <p>The frontend passes {@code linkToken} directly to the Plaid Link SDK to open the
 * institution-connection UI. The {@code expiration} field is informational; the frontend
 * should complete the Link flow before this timestamp.
 *
 * @param linkToken  opaque Plaid link token for the frontend SDK
 * @param expiration ISO-8601 timestamp at which the link token expires (typically 30 minutes)
 */
public record PlaidLinkTokenResponse(String linkToken, String expiration) {}
