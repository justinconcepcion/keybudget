package com.keybudget.integration.provider.coinbase;

import com.keybudget.integration.IntegrationProvider;
import com.keybudget.integration.ProviderType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Coinbase integration provider — stub implementation.
 * Full implementation will use the Coinbase Advanced Trade API (OAuth 2.0).
 * Note: Coinbase API usage may incur costs at higher request volumes; review their
 * developer pricing before enabling in production.
 */
@Service
public class CoinbaseProvider implements IntegrationProvider {

    @Override
    public ProviderType getProviderType() {
        return ProviderType.COINBASE;
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
