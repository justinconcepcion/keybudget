package com.keybudget.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keybudget.integration.dto.*;
import com.keybudget.integration.exception.ProviderException;
import com.keybudget.shared.ResourceNotFoundException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(IntegrationController.class)
class IntegrationControllerTest {

    private static final long USER_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IntegrationOrchestrationService orchestrationService;

    @MockBean
    private com.keybudget.integration.provider.plaid.PlaidService plaidService;

    // -------------------------------------------------------------------------
    // POST /api/v1/integrations/connect
    // -------------------------------------------------------------------------

    @Nested
    class ConnectProvider {

        @Test
        void connect_givenValidRequest_201() throws Exception {
            AccountResponse accountResponse = new AccountResponse(
                    1L, 10L, ProviderType.BITCOIN_WALLET, AccountType.CRYPTO_WALLET,
                    "Bitcoin Wallet (...abc123)", "BTC",
                    new BigDecimal("1.00000000"), new BigDecimal("60000.00"),
                    Instant.now(), true
            );
            when(orchestrationService.connectProvider(eq(USER_ID), any(ConnectAccountRequest.class)))
                    .thenReturn(List.of(accountResponse));

            mockMvc.perform(post("/api/v1/integrations/connect")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ConnectAccountRequest(
                                            ProviderType.BITCOIN_WALLET,
                                            Map.of("address", "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq")
                                    )
                            )))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$[0].providerType").value("BITCOIN_WALLET"))
                    .andExpect(jsonPath("$[0].currency").value("BTC"));
        }

        @Test
        void connect_givenMissingProviderType_400() throws Exception {
            mockMvc.perform(post("/api/v1/integrations/connect")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"credentials\":{\"address\":\"bc1q...\"}}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void connect_givenMissingCredentials_400() throws Exception {
            mockMvc.perform(post("/api/v1/integrations/connect")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"providerType\":\"BITCOIN_WALLET\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
        }

        @Test
        void connect_givenNoJwt_401() throws Exception {
            mockMvc.perform(post("/api/v1/integrations/connect")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ConnectAccountRequest(
                                            ProviderType.BITCOIN_WALLET,
                                            Map.of("address", "bc1qtest")
                                    )
                            )))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void connect_givenProviderThrows_502() throws Exception {
            when(orchestrationService.connectProvider(eq(USER_ID), any(ConnectAccountRequest.class)))
                    .thenThrow(new ProviderException(
                            ProviderType.BITCOIN_WALLET, "Blockstream returned 503"));

            mockMvc.perform(post("/api/v1/integrations/connect")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ConnectAccountRequest(
                                            ProviderType.BITCOIN_WALLET,
                                            Map.of("address", "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq")
                                    )
                            )))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.error").value("PROVIDER_ERROR"));
        }

        @Test
        void connect_givenAlreadyConnected_400() throws Exception {
            when(orchestrationService.connectProvider(eq(USER_ID), any(ConnectAccountRequest.class)))
                    .thenThrow(new IllegalArgumentException("Provider already connected: BITCOIN_WALLET"));

            mockMvc.perform(post("/api/v1/integrations/connect")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ConnectAccountRequest(
                                            ProviderType.BITCOIN_WALLET,
                                            Map.of("address", "bc1qar0srrr7xfkvy5l643lydnw9re59gtzzwf5mdq")
                                    )
                            )))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/integrations/accounts
    // -------------------------------------------------------------------------

    @Nested
    class GetAccounts {

        @Test
        void getAccounts_givenValidJwt_200() throws Exception {
            AccountResponse accountResponse = new AccountResponse(
                    1L, 10L, ProviderType.BITCOIN_WALLET, AccountType.CRYPTO_WALLET,
                    "Bitcoin Wallet (...abc)", "BTC",
                    new BigDecimal("0.50000000"), new BigDecimal("30000.00"),
                    Instant.now(), true
            );
            when(orchestrationService.getAccounts(USER_ID)).thenReturn(List.of(accountResponse));

            mockMvc.perform(get("/api/v1/integrations/accounts")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].currency").value("BTC"))
                    .andExpect(jsonPath("$[0].active").value(true));
        }

        @Test
        void getAccounts_givenNoJwt_401() throws Exception {
            mockMvc.perform(get("/api/v1/integrations/accounts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getAccounts_givenServiceThrows_500() throws Exception {
            when(orchestrationService.getAccounts(USER_ID))
                    .thenThrow(new RuntimeException("Database connection lost"));

            mockMvc.perform(get("/api/v1/integrations/accounts")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID))))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/integrations/providers
    // -------------------------------------------------------------------------

    @Nested
    class GetProviders {

        @Test
        void getProviders_givenValidJwt_200() throws Exception {
            ProviderStatusResponse statusResponse = new ProviderStatusResponse(
                    10L, ProviderType.BITCOIN_WALLET, SyncStatus.ACTIVE,
                    Instant.now(), null, 1
            );
            when(orchestrationService.getProviders(USER_ID)).thenReturn(List.of(statusResponse));

            mockMvc.perform(get("/api/v1/integrations/providers")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].providerType").value("BITCOIN_WALLET"))
                    .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$[0].accountCount").value(1));
        }

        @Test
        void getProviders_givenNoJwt_401() throws Exception {
            mockMvc.perform(get("/api/v1/integrations/providers"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/integrations/providers/{credentialId}
    // -------------------------------------------------------------------------

    @Nested
    class DisconnectProvider {

        @Test
        void disconnectProvider_givenValidId_204() throws Exception {
            doNothing().when(orchestrationService).disconnectProvider(USER_ID, 10L);

            mockMvc.perform(delete("/api/v1/integrations/providers/10")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void disconnectProvider_givenNotFound_404() throws Exception {
            doThrow(new ResourceNotFoundException("Credential not found: 99"))
                    .when(orchestrationService).disconnectProvider(USER_ID, 99L);

            mockMvc.perform(delete("/api/v1/integrations/providers/99")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("NOT_FOUND"));
        }

        @Test
        void disconnectProvider_givenNoJwt_401() throws Exception {
            mockMvc.perform(delete("/api/v1/integrations/providers/10")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/integrations/providers/{credentialId}/sync
    // -------------------------------------------------------------------------

    @Nested
    class SyncProvider {

        @Test
        void syncProvider_givenValidId_200() throws Exception {
            SyncResultResponse syncResult = new SyncResultResponse(
                    ProviderType.BITCOIN_WALLET, Instant.now(), 1, SyncStatus.ACTIVE, null
            );
            when(orchestrationService.syncProvider(USER_ID, 10L)).thenReturn(syncResult);

            mockMvc.perform(post("/api/v1/integrations/providers/10/sync")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.providerType").value("BITCOIN_WALLET"))
                    .andExpect(jsonPath("$.accountsUpdated").value(1))
                    .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void syncProvider_givenCredentialNotFound_404() throws Exception {
            when(orchestrationService.syncProvider(USER_ID, 99L))
                    .thenThrow(new ResourceNotFoundException("Credential not found: 99"));

            mockMvc.perform(post("/api/v1/integrations/providers/99/sync")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID)))
                            .with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test
        void syncProvider_givenNoJwt_401() throws Exception {
            mockMvc.perform(post("/api/v1/integrations/providers/10/sync")
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/integrations/net-worth
    // -------------------------------------------------------------------------

    @Nested
    class GetNetWorth {

        @Test
        void getNetWorth_givenValidJwt_200() throws Exception {
            NetWorthResponse netWorthResponse = new NetWorthResponse(
                    new BigDecimal("60000.00"),
                    List.of(new ProviderTotal(ProviderType.BITCOIN_WALLET,
                            new BigDecimal("60000.00"), 1)),
                    List.of(new AccountTypeTotal(AccountType.CRYPTO_WALLET,
                            new BigDecimal("60000.00"), 1)),
                    Instant.now()
            );
            when(orchestrationService.getNetWorth(USER_ID)).thenReturn(netWorthResponse);

            mockMvc.perform(get("/api/v1/integrations/net-worth")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalNetWorthUsd").value(60000.00));
        }

        @Test
        void getNetWorth_givenNoJwt_401() throws Exception {
            mockMvc.perform(get("/api/v1/integrations/net-worth"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void getNetWorth_givenServiceThrows_500() throws Exception {
            when(orchestrationService.getNetWorth(USER_ID))
                    .thenThrow(new RuntimeException("Unexpected failure"));

            mockMvc.perform(get("/api/v1/integrations/net-worth")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID))))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("INTERNAL_ERROR"));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/integrations/net-worth/history
    // -------------------------------------------------------------------------

    @Nested
    class GetNetWorthHistory {

        @Test
        void getNetWorthHistory_givenValidDays_200() throws Exception {
            NetWorthHistoryResponse historyResponse = new NetWorthHistoryResponse(
                    List.of(new NetWorthDataPoint(LocalDate.now(), new BigDecimal("60000.00")))
            );
            when(orchestrationService.getNetWorthHistory(USER_ID, 30)).thenReturn(historyResponse);

            mockMvc.perform(get("/api/v1/integrations/net-worth/history")
                            .param("days", "30")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.dataPoints").isArray())
                    .andExpect(jsonPath("$.dataPoints[0].totalUsd").value(60000.00));
        }

        @Test
        void getNetWorthHistory_givenDefaultDays_200() throws Exception {
            NetWorthHistoryResponse historyResponse = new NetWorthHistoryResponse(List.of());
            when(orchestrationService.getNetWorthHistory(USER_ID, 30)).thenReturn(historyResponse);

            mockMvc.perform(get("/api/v1/integrations/net-worth/history")
                            .with(jwt().jwt(j -> j.claim("userId", USER_ID))))
                    .andExpect(status().isOk());
        }

        @Test
        void getNetWorthHistory_givenNoJwt_401() throws Exception {
            mockMvc.perform(get("/api/v1/integrations/net-worth/history"))
                    .andExpect(status().isUnauthorized());
        }
    }
}
