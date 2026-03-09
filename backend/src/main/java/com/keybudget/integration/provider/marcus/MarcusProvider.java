package com.keybudget.integration.provider.marcus;

import com.keybudget.integration.AccountType;
import com.keybudget.integration.IntegrationProvider;
import com.keybudget.integration.ProviderType;
import com.keybudget.integration.exception.ProviderException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Marcus by Goldman Sachs savings account provider.
 * Uses Plaid for bank data access (Dev tier: $0/month, free sandbox).
 *
 * <p>To enable: sign up at https://dashboard.plaid.com (free),
 * set PLAID_CLIENT_ID and PLAID_SECRET env vars, then use Plaid Link
 * on the frontend to connect the Marcus account.
 *
 * <p>Current status: sandbox-ready stub. Returns a demo account when
 * Plaid is not configured; real implementation requires Plaid Link flow.
 */
@Service
public class MarcusProvider implements IntegrationProvider {

    @Override
    public ProviderType getProviderType() {
        return ProviderType.MARCUS;
    }

    @Override
    public List<DiscoveredAccount> connect(Map<String, String> credentials) {
        // In production, this would exchange a Plaid public_token for an access_token
        // and then call /accounts/get to discover savings accounts.
        // For the POC, return a demo account so the UI works end-to-end.
        String accessToken = credentials.get("plaidAccessToken");
        if (accessToken == null || accessToken.isBlank()) {
            throw new ProviderException(ProviderType.MARCUS, "Marcus requires Plaid Link integration. " +
                    "Sign up at https://dashboard.plaid.com (free) to enable.");
        }

        return List.of(new DiscoveredAccount(
                "marcus-savings-demo",
                "Marcus High-Yield Savings",
                AccountType.SAVINGS,
                "USD",
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));
    }

    @Override
    public List<ProviderBalance> syncBalances(String decryptedCredentialData) {
        // In production, this would call Plaid /accounts/balance/get
        // For the POC, return zero balance
        return List.of(new ProviderBalance(
                "marcus-savings-demo",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                Instant.now()
        ));
    }

    @Override
    public boolean validateCredential(String decryptedCredentialData) {
        // Would call Plaid /item/get to verify the token is still valid
        return decryptedCredentialData != null && !decryptedCredentialData.isBlank();
    }
}
