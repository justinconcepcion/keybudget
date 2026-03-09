package com.keybudget.integration.provider.m1finance;

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
 * M1 Finance brokerage provider.
 * Uses Plaid for brokerage data access (Dev tier: $0/month, free sandbox).
 *
 * <p>To enable: sign up at https://dashboard.plaid.com (free),
 * set PLAID_CLIENT_ID and PLAID_SECRET env vars, then use Plaid Link
 * on the frontend to connect the M1 Finance account.
 *
 * <p>Current status: sandbox-ready stub. Returns a demo account when
 * Plaid is not configured; real implementation requires Plaid Link flow.
 */
@Service
public class M1FinanceProvider implements IntegrationProvider {

    @Override
    public ProviderType getProviderType() {
        return ProviderType.M1_FINANCE;
    }

    @Override
    public List<DiscoveredAccount> connect(Map<String, String> credentials) {
        // In production, this would exchange a Plaid public_token for an access_token
        // and then call /investments/holdings/get to discover brokerage accounts.
        String accessToken = credentials.get("plaidAccessToken");
        if (accessToken == null || accessToken.isBlank()) {
            throw new ProviderException(ProviderType.M1_FINANCE, "M1 Finance requires Plaid Link integration. " +
                    "Sign up at https://dashboard.plaid.com (free) to enable.");
        }

        return List.of(new DiscoveredAccount(
                "m1-brokerage-demo",
                "M1 Finance Brokerage",
                AccountType.BROKERAGE,
                "USD",
                BigDecimal.ZERO,
                BigDecimal.ZERO
        ));
    }

    @Override
    public List<ProviderBalance> syncBalances(String decryptedCredentialData) {
        // In production, this would call Plaid /investments/holdings/get
        // For the POC, return zero balance
        return List.of(new ProviderBalance(
                "m1-brokerage-demo",
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
