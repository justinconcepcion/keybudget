package com.keybudget.integration.provider.coinbase.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A single account (wallet) entry from the Coinbase {@code /v2/accounts} response.
 *
 * @param id            Coinbase's own unique account identifier
 * @param name          human-readable wallet name (e.g., "BTC Wallet")
 * @param balance       native-currency balance (amount + currency ticker)
 * @param nativeBalance USD-equivalent balance as reported by Coinbase
 * @param type          account type string (e.g., "wallet", "fiat")
 * @param currency      currency metadata (code and full name)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinbaseAccount(
        String id,
        String name,
        CoinbaseBalance balance,
        @JsonProperty("native_balance") CoinbaseBalance nativeBalance,
        String type,
        CoinbaseCurrency currency
) {}
