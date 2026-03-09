package com.keybudget.integration.provider.bitcoin;

import com.keybudget.integration.IntegrationProvider;
import com.keybudget.integration.ProviderType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Bitcoin watch-only wallet provider — stub implementation.
 * Full implementation will use a public blockchain API (e.g., Blockstream Esplora or
 * Mempool.space) to fetch UTXO balance for a given address without requiring a private key.
 * Public blockchain APIs are free; rate limits apply.
 */
@Service
public class BitcoinWalletProvider implements IntegrationProvider {

    @Override
    public ProviderType getProviderType() {
        return ProviderType.BITCOIN_WALLET;
    }

    @Override
    public List<DiscoveredAccount> connect(Map<String, String> credentials) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<ProviderBalance> syncBalances(String decryptedCredentialData) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean validateCredential(String decryptedCredentialData) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
