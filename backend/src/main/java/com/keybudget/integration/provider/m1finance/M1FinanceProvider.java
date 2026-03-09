package com.keybudget.integration.provider.m1finance;

import com.keybudget.integration.IntegrationProvider;
import com.keybudget.integration.ProviderType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * M1 Finance brokerage provider — stub implementation.
 * Note: M1 Finance does not offer a public API. Full implementation will require
 * evaluating unofficial approaches (screen scraping, Plaid, or a future official API).
 * Plaid integration carries per-account costs; flag to user before enabling.
 */
@Service
public class M1FinanceProvider implements IntegrationProvider {

    @Override
    public ProviderType getProviderType() {
        return ProviderType.M1_FINANCE;
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
