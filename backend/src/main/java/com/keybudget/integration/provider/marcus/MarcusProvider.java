package com.keybudget.integration.provider.marcus;

import com.keybudget.integration.IntegrationProvider;
import com.keybudget.integration.ProviderType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Marcus by Goldman Sachs savings account provider — stub implementation.
 * Note: Marcus does not offer a public API. Full implementation will require
 * Plaid (paid, per-account pricing) or an official bank integration when available.
 * Flag Plaid costs to the user before enabling.
 */
@Service
public class MarcusProvider implements IntegrationProvider {

    @Override
    public ProviderType getProviderType() {
        return ProviderType.MARCUS;
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
