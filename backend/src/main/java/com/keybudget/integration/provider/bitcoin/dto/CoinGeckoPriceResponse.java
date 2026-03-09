package com.keybudget.integration.provider.bitcoin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the JSON response from the CoinGecko simple price endpoint:
 * {@code GET /api/v3/simple/price?ids=bitcoin&vs_currencies=usd}
 *
 * <p>Example response:
 * <pre>{@code {"bitcoin":{"usd":63012.00}}}</pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CoinGeckoPriceResponse(
        @JsonProperty("bitcoin")
        BitcoinPrice bitcoin
) {

    /**
     * BTC price data in the requested vs-currencies.
     *
     * @param usd current BTC price in USD
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BitcoinPrice(
            @JsonProperty("usd") double usd
    ) {}
}
