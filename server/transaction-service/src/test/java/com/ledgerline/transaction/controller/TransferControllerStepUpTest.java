package com.ledgerline.transaction.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ledgerline.shared.exception.GlobalExceptionHandler;
import com.ledgerline.shared.security.StepUpConstants;
import com.ledgerline.transaction.client.AccountClient;
import com.ledgerline.transaction.client.CustomerClient.CustomerSummary;
import com.ledgerline.transaction.dto.AccountBalanceResponse;
import com.ledgerline.transaction.dto.TransactionResponse;
import com.ledgerline.transaction.dto.TransferResponse;
import com.ledgerline.transaction.mapper.TransactionMapper;
import com.ledgerline.transaction.security.StepUpTokenService;
import com.ledgerline.transaction.service.PaymentPolicy;
import com.ledgerline.transaction.service.TransactionService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Proves that targetAccountId cannot bypass the re-authentication requirement that the BFF used to
 * apply only to targetAccountNumber requests.
 */
class TransferControllerStepUpTest {

    private static final String SECRET = "test-jwt-secret-that-is-long-enough-0123456789abcdef";
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final TransactionService transactionService = mock(TransactionService.class);
    private final TransactionMapper mapper = mock(TransactionMapper.class);
    private final AccountClient accountClient = mock(AccountClient.class);
    private final CallerContext caller = mock(CallerContext.class);
    private final PaymentPolicy paymentPolicy = mock(PaymentPolicy.class);
    private final StepUpTokenService stepUpTokens = new StepUpTokenService(SECRET);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        TransferController controller =
                new TransferController(
                        transactionService,
                        mapper,
                        accountClient,
                        caller,
                        paymentPolicy,
                        stepUpTokens);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();

        when(caller.hasRole("CUSTOMER")).thenReturn(true);
        when(caller.hasRole("ADMIN")).thenReturn(false);
        when(caller.userId()).thenReturn("42");
        when(caller.customer()).thenReturn(new CustomerSummary(7L, 42L, "VERIFIED"));
        when(accountClient.getAccount(10L)).thenReturn(account(10L, 7L));
        when(accountClient.getAccount(20L)).thenReturn(account(20L, 8L));
        when(accountClient.getAccount(11L)).thenReturn(account(11L, 7L));
        when(accountClient.getAccountByNumber("LDG0123456789")).thenReturn(account(20L, 8L));
        when(transactionService.execute(any()))
                .thenReturn(
                        TransactionResponse.builder()
                                .id("txn-1")
                                .type("TRANSFER")
                                .status("COMPLETED")
                                .build());
        when(mapper.toTransferResponse(any()))
                .thenReturn(TransferResponse.builder().status("COMPLETED").build());
    }

    @Test
    void externalTargetAccountIdWithoutTokenRequiresStepUp() throws Exception {
        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header("Idempotency-Key", "step-up-id-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("20", null)))
                .andExpect(status().is(428));

        verify(transactionService, never()).execute(any());
    }

    @Test
    void externalTargetAccountNumberWithoutTokenRequiresStepUp() throws Exception {
        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header("Idempotency-Key", "step-up-number-1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body(null, "LDG0123456789")))
                .andExpect(status().is(428));
    }

    @Test
    void externalTargetAccountIdWithAValidStepUpTokenSucceeds() throws Exception {
        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header("Idempotency-Key", "step-up-id-2")
                                .header("X-Step-Up-Token", stepUpToken("step-up", 120))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("20", null)))
                .andExpect(status().isCreated());
    }

    @Test
    void aNormalAccessTokenCannotPayAnExternalAccount() throws Exception {
        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header("Idempotency-Key", "step-up-id-3")
                                .header("X-Step-Up-Token", stepUpToken("access", 120))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("20", null)))
                .andExpect(status().is(428));
    }

    @Test
    void anExpiredStepUpTokenIsRejected() throws Exception {
        Instant now = Instant.now();
        String token =
                Jwts.builder()
                        .subject("42")
                        .audience()
                        .add(StepUpConstants.AUDIENCE)
                        .and()
                        .claim("scope", "step-up")
                        .issuedAt(Date.from(now.minusSeconds(600)))
                        .expiration(Date.from(now.minusSeconds(300)))
                        .signWith(KEY)
                        .compact();

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header("Idempotency-Key", "step-up-id-4")
                                .header("X-Step-Up-Token", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("20", null)))
                .andExpect(status().is(428));
    }

    @Test
    void aTokenForAnotherUserIsRejected() throws Exception {
        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header("Idempotency-Key", "step-up-id-5")
                                .header("X-Step-Up-Token", stepUpToken("7", "step-up", 120))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("20", null)))
                .andExpect(status().is(428));
    }

    @Test
    void aTransferBetweenOwnAccountsDoesNotRequireStepUp() throws Exception {
        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header("Idempotency-Key", "step-up-id-6")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("11", null)))
                .andExpect(status().isCreated());
    }

    @Test
    void anAdminCanTransferWithoutStepUp() throws Exception {
        when(caller.hasRole("CUSTOMER")).thenReturn(false);
        when(caller.hasRole("ADMIN")).thenReturn(true);

        mockMvc.perform(
                        post("/api/v1/transfers")
                                .header("Idempotency-Key", "step-up-id-7")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body("20", null)))
                .andExpect(status().isCreated());
    }

    private static AccountBalanceResponse account(Long id, Long customerId) {
        return AccountBalanceResponse.builder()
                .id(id)
                .customerId(customerId)
                .status("ACTIVE")
                .currency("COP")
                .build();
    }

    private static String body(String targetAccountId, String targetAccountNumber) {
        String target =
                targetAccountId != null
                        ? "\"targetAccountId\":" + targetAccountId
                        : "\"targetAccountNumber\":\"" + targetAccountNumber + "\"";
        return """
               {
                 "sourceAccountId": 10,
                 %s,
                 "amount": 1000,
                 "currency": "COP",
                 "description": "test"
               }
               """
                .formatted(target);
    }

    private static String stepUpToken(String scope, long lifetimeSeconds) {
        return stepUpToken("42", scope, lifetimeSeconds);
    }

    private static String stepUpToken(String subject, String scope, long lifetimeSeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject)
                .audience()
                .add(StepUpConstants.AUDIENCE)
                .and()
                .claim("scope", scope)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(lifetimeSeconds)))
                .signWith(KEY)
                .compact();
    }
}
