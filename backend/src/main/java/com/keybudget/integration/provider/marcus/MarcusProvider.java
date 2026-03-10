package com.keybudget.integration.provider.marcus;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.AccountType;
import com.keybudget.integration.IntegrationProvider;
import com.keybudget.integration.ProviderType;
import com.keybudget.integration.exception.ProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Marcus by Goldman Sachs savings account provider — manual balance entry mode.
 *
 * <p>Marcus does not expose a public API for personal use, and Plaid requires
 * a business application for Development/Production access. This provider accepts
 * a user-entered balance that can be updated periodically via the sync/edit flow.
 *
 * <p>Credentials map: {@code {"balance": "50000.00", "accountName": "Marcus Savings"}}
 */
@Slf4j
@Service
public class MarcusProvider implements IntegrationProvider {

    private static final String KEY_BALANCE = "balance";
    private static final String KEY_ACCOUNT_NAME = "accountName";
    private static final String DEFAULT_ACCOUNT_NAME = "Marcus High-Yield Savings";

    private final ObjectMapper objectMapper;

    public MarcusProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ProviderType getProviderType() {
        return ProviderType.MARCUS;
    }

    @Override
    public List<DiscoveredAccount> connect(Map<String, String> credentials) {
        BigDecimal balance = parseBalance(credentials.get(KEY_BALANCE));
        String accountName = credentials.getOrDefault(KEY_ACCOUNT_NAME, DEFAULT_ACCOUNT_NAME);
        if (accountName.isBlank()) accountName = DEFAULT_ACCOUNT_NAME;

        log.info("Marcus connected (manual): accountName={}, balance={}",
                accountName, balance);

        return List.of(new DiscoveredAccount(
                "marcus-manual",
                accountName,
                AccountType.SAVINGS,
                "USD",
                balance,
                balance
        ));
    }

    @Override
    public List<ProviderBalance> syncBalances(String decryptedCredentialData) {
        Map<String, String> creds = parseCredentialJson(decryptedCredentialData);
        BigDecimal balance = parseBalance(creds.get(KEY_BALANCE));

        return List.of(new ProviderBalance(
                "marcus-manual",
                balance,
                balance,
                Instant.now()
        ));
    }

    @Override
    public boolean validateCredential(String decryptedCredentialData) {
        try {
            Map<String, String> creds = parseCredentialJson(decryptedCredentialData);
            parseBalance(creds.get(KEY_BALANCE));
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private BigDecimal parseBalance(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Balance is required for Marcus (manual entry)");
        }
        try {
            BigDecimal balance = new BigDecimal(raw.trim());
            if (balance.signum() < 0) {
                throw new IllegalArgumentException("Balance cannot be negative");
            }
            return balance.setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid balance format: " + raw);
        }
    }

    private Map<String, String> parseCredentialJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new ProviderException(ProviderType.MARCUS,
                    "Failed to parse stored credential JSON", ex);
        }
    }
}
