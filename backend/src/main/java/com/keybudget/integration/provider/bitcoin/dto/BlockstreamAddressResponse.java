package com.keybudget.integration.provider.bitcoin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Maps the JSON response from the Blockstream Esplora address endpoint:
 * {@code GET /api/address/{address}}
 *
 * <p>Only the fields relevant to balance calculation are mapped. Unknown fields
 * are silently ignored via {@link JsonIgnoreProperties}.
 *
 * <p>Balance in satoshis = {@code chain_stats.funded_txo_sum - chain_stats.spent_txo_sum}.
 * Divide by 100,000,000 to convert to BTC.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BlockstreamAddressResponse(
        String address,

        @JsonProperty("chain_stats")
        ChainStats chainStats,

        @JsonProperty("mempool_stats")
        MempoolStats mempoolStats
) {

    /**
     * Confirmed (on-chain) transaction statistics for the address.
     *
     * @param fundedTxoCount  number of UTXOs that funded this address
     * @param fundedTxoSum    total satoshis received (confirmed)
     * @param spentTxoCount   number of UTXOs spent from this address
     * @param spentTxoSum     total satoshis spent (confirmed)
     * @param txCount         total confirmed transaction count
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChainStats(
            @JsonProperty("funded_txo_count") long fundedTxoCount,
            @JsonProperty("funded_txo_sum")   long fundedTxoSum,
            @JsonProperty("spent_txo_count")  long spentTxoCount,
            @JsonProperty("spent_txo_sum")    long spentTxoSum,
            @JsonProperty("tx_count")         long txCount
    ) {}

    /**
     * Unconfirmed (mempool) transaction statistics for the address.
     * These represent pending transactions not yet included in a block.
     *
     * @param fundedTxoCount  unconfirmed UTXOs funding this address
     * @param fundedTxoSum    unconfirmed satoshis incoming
     * @param spentTxoCount   unconfirmed UTXOs spent from this address
     * @param spentTxoSum     unconfirmed satoshis outgoing
     * @param txCount         total unconfirmed transaction count
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MempoolStats(
            @JsonProperty("funded_txo_count") long fundedTxoCount,
            @JsonProperty("funded_txo_sum")   long fundedTxoSum,
            @JsonProperty("spent_txo_count")  long spentTxoCount,
            @JsonProperty("spent_txo_sum")    long spentTxoSum,
            @JsonProperty("tx_count")         long txCount
    ) {}
}
