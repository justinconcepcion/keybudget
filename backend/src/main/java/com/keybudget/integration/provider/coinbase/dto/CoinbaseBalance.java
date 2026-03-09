package com.keybudget.integration.provider.coinbase.dto;

/**
 * A balance amount paired with its currency, as returned by the Coinbase API.
 * Both {@code balance} and {@code nativeBalance} fields on {@link CoinbaseAccount}
 * use this structure.
 *
 * @param amount   decimal string representation of the balance (e.g., "0.05432100")
 * @param currency ISO 4217 currency code or crypto ticker (e.g., "BTC", "USD")
 */
public record CoinbaseBalance(String amount, String currency) {}
