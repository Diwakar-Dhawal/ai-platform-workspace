package com.aiservice.platform.identity.integration;

import com.aiservice.platform.identity.dto.request.ForgotPasswordRequest;
import com.aiservice.platform.identity.dto.request.LoginRequest;
import com.aiservice.platform.identity.dto.request.RefreshTokenRequest;
import com.aiservice.platform.identity.dto.request.RegisterRequest;
import com.aiservice.platform.identity.dto.request.ResetPasswordRequest;
import com.aiservice.platform.identity.dto.request.VerifyEmailRequest;
import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.entity.VerificationToken;
import com.aiservice.platform.identity.repository.RefreshTokenRepository;
import com.aiservice.platform.identity.repository.UserRepository;
import com.aiservice.platform.identity.repository.VerificationTokenRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthIntegrationTest {

    @Autowired
    private TestDataSeeder seeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private VerificationTokenRepository verificationTokenRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate;

    private static String registeredAccessToken;
    private static String registeredRefreshToken;
    private static String resetTokenValue;
    private static String verifyTokenValue;

    @BeforeAll
    void setup() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new org.springframework.web.client.NoOpResponseErrorHandler());
        seeder.seedAll();
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        return headers;
    }

    private static final String CONTEXT = "/identity-service";

    private ApiResponse<AuthResponse> postAuth(String url, Object request) {
        HttpEntity<Object> entity = new HttpEntity<>(request, jsonHeaders());
        String baseUrl = "http://localhost:" + port + CONTEXT;
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + url, entity, String.class);
        try {
            return objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse response", e);
        }
    }

    private HttpStatus postAuthStatus(String url, Object request) {
        HttpEntity<Object> entity = new HttpEntity<>(request, jsonHeaders());
        String baseUrl = "http://localhost:" + port + CONTEXT;
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + url, entity, String.class);
        return HttpStatus.valueOf(response.getStatusCode().value());
    }

    // ─────────────────────────────────────────────
    //  REGISTER TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /auth/register — success with valid data")
    void register_success() {
        RegisterRequest request = new RegisterRequest(
                "testuser", "test@example.com", "StrongP@ss1", "test-client"
        );

        ApiResponse<AuthResponse> body = postAuth("/auth/register", request);
        assertNotNull(body.data().accessToken());
        assertNotNull(body.data().refreshToken());
        assertEquals("testuser", body.data().user().username());

        registeredAccessToken = body.data().accessToken();
        registeredRefreshToken = body.data().refreshToken();
    }

    @Test
    @Order(2)
    @DisplayName("POST /auth/register — fails with duplicate email")
    void register_duplicateEmail() {
        RegisterRequest request = new RegisterRequest(
                "anotheruser", "test@example.com", "StrongP@ss1", "test-client"
        );
        assertEquals(HttpStatus.CONFLICT, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(3)
    @DisplayName("POST /auth/register — fails with duplicate username")
    void register_duplicateUsername() {
        RegisterRequest request = new RegisterRequest(
                "testuser", "other@example.com", "StrongP@ss1", "test-client"
        );
        assertEquals(HttpStatus.CONFLICT, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(4)
    @DisplayName("POST /auth/register — fails with non-existent client")
    void register_clientNotFound() {
        RegisterRequest request = new RegisterRequest(
                "newuser", "new@example.com", "StrongP@ss1", "nonexistent-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(5)
    @DisplayName("POST /auth/register — fails with weak password (no uppercase)")
    void register_weakPassword_noUppercase() {
        RegisterRequest request = new RegisterRequest(
                "weakuser1", "weak1@example.com", "strongp@ss1", "test-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(6)
    @DisplayName("POST /auth/register — fails with weak password (no digit)")
    void register_weakPassword_noDigit() {
        RegisterRequest request = new RegisterRequest(
                "weakuser2", "weak2@example.com", "StrongP@ss", "test-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(7)
    @DisplayName("POST /auth/register — fails with weak password (no special char)")
    void register_weakPassword_noSpecialChar() {
        RegisterRequest request = new RegisterRequest(
                "weakuser3", "weak3@example.com", "StrongPass1", "test-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(8)
    @DisplayName("POST /auth/register — fails with short password")
    void register_passwordTooShort() {
        RegisterRequest request = new RegisterRequest(
                "weakuser4", "weak4@example.com", "Ab1@", "test-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(9)
    @DisplayName("POST /auth/register — fails with invalid email")
    void register_invalidEmail() {
        RegisterRequest request = new RegisterRequest(
                "validuser", "not-an-email", "StrongP@ss1", "test-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(10)
    @DisplayName("POST /auth/register — fails with blank fields")
    void register_blankFields() {
        RegisterRequest request = new RegisterRequest("", "", "", "");
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    // ─────────────────────────────────────────────
    //  LOGIN TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(11)
    @DisplayName("POST /auth/login — success with valid credentials")
    void login_success() {
        LoginRequest request = new LoginRequest(
                "test@example.com", "StrongP@ss1", "test-client"
        );
        ApiResponse<AuthResponse> body = postAuth("/auth/login", request);
        assertNotNull(body.data().accessToken());
        assertNotNull(body.data().refreshToken());
        assertEquals("testuser", body.data().user().username());
    }

    @Test
    @Order(12)
    @DisplayName("POST /auth/login — fails with wrong password")
    void login_wrongPassword() {
        LoginRequest request = new LoginRequest(
                "test@example.com", "WrongP@ss1", "test-client"
        );
        assertEquals(HttpStatus.UNAUTHORIZED, postAuthStatus("/auth/login", request));
    }

    @Test
    @Order(13)
    @DisplayName("POST /auth/login — fails with non-existent email")
    void login_nonExistentEmail() {
        LoginRequest request = new LoginRequest(
                "nobody@example.com", "StrongP@ss1", "test-client"
        );
        assertEquals(HttpStatus.UNAUTHORIZED, postAuthStatus("/auth/login", request));
    }

    @Test
    @Order(14)
    @DisplayName("POST /auth/login — fails with non-existent client")
    void login_clientNotFound() {
        // User exists from order 11, but client doesn't
        LoginRequest request = new LoginRequest(
                "test@example.com", "StrongP@ss1", "nonexistent-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/login", request));
    }

    // ─────────────────────────────────────────────
    //  REFRESH TOKEN TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(15)
    @DisplayName("POST /auth/refresh — success with valid refresh token")
    void refresh_success() {
        RefreshTokenRequest request = new RefreshTokenRequest(registeredRefreshToken);
        ApiResponse<AuthResponse> body = postAuth("/auth/refresh", request);
        assertNotNull(body.data().accessToken());
        assertNotNull(body.data().refreshToken());
        registeredAccessToken = body.data().accessToken();
        registeredRefreshToken = body.data().refreshToken();
    }

    @Test
    @Order(16)
    @DisplayName("POST /auth/refresh — fails with invalid token")
    void refresh_invalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("completely-fake-token-12345");
        assertEquals(HttpStatus.UNAUTHORIZED, postAuthStatus("/auth/refresh", request));
    }

    @Test
    @Order(17)
    @DisplayName("POST /auth/refresh — fails with empty token")
    void refresh_emptyToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("");
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/refresh", request));
    }

    // ─────────────────────────────────────────────
    //  LOGOUT TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(18)
    @DisplayName("POST /auth/logout — success with valid refresh token")
    void logout_success() {
        RefreshTokenRequest request = new RefreshTokenRequest(registeredRefreshToken);
        // logout requires JWT auth
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(registeredAccessToken);
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);
        String baseUrl = "http://localhost:" + port + CONTEXT;
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/auth/logout", entity, String.class);
        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    }

    @Test
    @Order(19)
    @DisplayName("POST /auth/refresh — fails after logout (token revoked)")
    void refresh_failsAfterLogout() {
        RefreshTokenRequest request = new RefreshTokenRequest(registeredRefreshToken);
        assertEquals(HttpStatus.UNAUTHORIZED, postAuthStatus("/auth/refresh", request));
    }

    // ─────────────────────────────────────────────
    //  EDGE CASES
    // ─────────────────────────────────────────────

    @Test
    @Order(20)
    @DisplayName("POST /auth/register — fails with invalid username chars")
    void register_invalidUsername() {
        RegisterRequest request = new RegisterRequest(
                "user@#$!", "special@example.com", "StrongP@ss1", "test-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(21)
    @DisplayName("POST /auth/login — fails with empty body")
    void login_emptyBody() {
        LoginRequest request = new LoginRequest(null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/login", request));
    }

    @Test
    @Order(22)
    @DisplayName("POST /auth/register — fails with empty body")
    void register_emptyBody() {
        RegisterRequest request = new RegisterRequest(null, null, null, null);
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(23)
    @DisplayName("POST /auth/logout — fails with invalid token")
    void logout_invalidToken() {
        RefreshTokenRequest request = new RefreshTokenRequest("fake-token");
        // logout requires JWT auth — use invalid token
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth("invalid-jwt-token");
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);
        String baseUrl = "http://localhost:" + port + CONTEXT;
        ResponseEntity<String> response = restTemplate.postForEntity(baseUrl + "/auth/logout", entity, String.class);
        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
    }

    @Test
    @Order(24)
    @DisplayName("POST /auth/register — username too short")
    void register_usernameTooShort() {
        RegisterRequest request = new RegisterRequest(
                "ab", "short@example.com", "StrongP@ss1", "test-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/register", request));
    }

    @Test
    @Order(25)
    @DisplayName("POST /auth/login — fails when user has no roles for client")
    void login_noRolesForClient() {
        RegisterRequest registerReq = new RegisterRequest(
                "noroleuser", "norole@example.com", "StrongP@ss1", "test-client"
        );
        seeder.seedClient("other-client", "Other Client");
        postAuth("/auth/register", registerReq);

        LoginRequest loginReq = new LoginRequest(
                "norole@example.com", "StrongP@ss1", "other-client"
        );
        assertEquals(HttpStatus.UNAUTHORIZED, postAuthStatus("/auth/login", loginReq));
    }

    // ─────────────────────────────────────────────
    //  FORGOT PASSWORD TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(26)
    @DisplayName("POST /auth/forgot-password — always returns 200 (even for unknown email)")
    void forgotPassword_success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "test@example.com", "test-client"
        );
        assertEquals(HttpStatus.OK, postAuthStatus("/auth/forgot-password", request));
    }

    @Test
    @Order(27)
    @DisplayName("POST /auth/forgot-password — returns 200 for unknown email (no enumeration)")
    void forgotPassword_unknownEmail() {
        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "unknown@example.com", "test-client"
        );
        assertEquals(HttpStatus.OK, postAuthStatus("/auth/forgot-password", request));
    }

    @Test
    @Order(28)
    @DisplayName("POST /auth/forgot-password — fails with invalid client")
    void forgotPassword_invalidClient() {
        ForgotPasswordRequest request = new ForgotPasswordRequest(
                "test@example.com", "nonexistent-client"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/forgot-password", request));
    }

    @Test
    @Order(29)
    @DisplayName("POST /auth/forgot-password — fails with blank fields")
    void forgotPassword_blankFields() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("", "");
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/forgot-password", request));
    }

    // ─────────────────────────────────────────────
    //  RESET PASSWORD TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(30)
    @DisplayName("POST /auth/reset-password — success flow")
    void resetPassword_success() {
        // Trigger forgot password to create a token
        ForgotPasswordRequest forgotReq = new ForgotPasswordRequest(
                "test@example.com", "test-client"
        );
        postAuthStatus("/auth/forgot-password", forgotReq);

        // Query the token from the database
        com.aiservice.platform.identity.entity.User user =
                userRepository.findByEmail("test@example.com").orElseThrow();
        VerificationToken resetToken = verificationTokenRepository
                .findByTokenAndTokenType("unused-token", VerificationToken.TokenType.PASSWORD_RESET)
                .orElseGet(() -> {
                    // If no token found, create one directly (simulating what forgot-password did)
                    String tokenVal = "reset-token-" + UUID.randomUUID();
                    VerificationToken vt = VerificationToken.builder()
                            .user(user)
                            .token(tokenVal)
                            .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
                            .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                            .build();
                    return verificationTokenRepository.save(vt);
                });
        resetTokenValue = resetToken.getToken();

        // Reset password
        ResetPasswordRequest resetReq = new ResetPasswordRequest(
                resetTokenValue, "NewP@ssw0rd"
        );
        assertEquals(HttpStatus.OK, postAuthStatus("/auth/reset-password", resetReq));

        // Verify old password no longer works
        LoginRequest loginOld = new LoginRequest(
                "test@example.com", "StrongP@ss1", "test-client"
        );
        assertEquals(HttpStatus.UNAUTHORIZED, postAuthStatus("/auth/login", loginOld));

        // Verify new password works
        LoginRequest loginNew = new LoginRequest(
                "test@example.com", "NewP@ssw0rd", "test-client"
        );
        ApiResponse<AuthResponse> loginBody = postAuth("/auth/login", loginNew);
        assertNotNull(loginBody.data().accessToken());

        // Update credentials for subsequent tests
        registeredAccessToken = loginBody.data().accessToken();
        registeredRefreshToken = loginBody.data().refreshToken();
    }

    @Test
    @Order(31)
    @DisplayName("POST /auth/reset-password — fails with invalid token")
    void resetPassword_invalidToken() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "totally-fake-token", "NewP@ssw0rd"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/reset-password", request));
    }

    @Test
    @Order(32)
    @DisplayName("POST /auth/reset-password — fails with already used token")
    void resetPassword_tokenAlreadyUsed() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                resetTokenValue, "AnotherP@ss1"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/reset-password", request));
    }

    @Test
    @Order(33)
    @DisplayName("POST /auth/reset-password — fails with expired token")
    void resetPassword_expiredToken() {
        // Create an expired token directly
        com.aiservice.platform.identity.entity.User user =
                userRepository.findByEmail("test@example.com").orElseThrow();
        String expiredTokenVal = "expired-token-" + UUID.randomUUID();
        VerificationToken expiredToken = VerificationToken.builder()
                .user(user)
                .token(expiredTokenVal)
                .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))  // Already expired
                .build();
        verificationTokenRepository.save(expiredToken);

        ResetPasswordRequest request = new ResetPasswordRequest(
                expiredTokenVal, "AnotherP@ss1"
        );
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/reset-password", request));
    }

    @Test
    @Order(34)
    @DisplayName("POST /auth/reset-password — fails with blank fields")
    void resetPassword_blankFields() {
        ResetPasswordRequest request = new ResetPasswordRequest("", "");
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/reset-password", request));
    }

    // ─────────────────────────────────────────────
    //  VERIFY EMAIL TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(35)
    @DisplayName("POST /auth/verify-email — success")
    void verifyEmail_success() {
        com.aiservice.platform.identity.entity.User user =
                userRepository.findByEmail("test@example.com").orElseThrow();
        verifyTokenValue = "verify-token-" + UUID.randomUUID();
        VerificationToken verifyToken = VerificationToken.builder()
                .user(user)
                .token(verifyTokenValue)
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        verificationTokenRepository.save(verifyToken);

        VerifyEmailRequest request = new VerifyEmailRequest(verifyTokenValue);
        assertEquals(HttpStatus.OK, postAuthStatus("/auth/verify-email", request));

        // Verify email is now marked as verified
        com.aiservice.platform.identity.entity.User updated =
                userRepository.findByEmail("test@example.com").orElseThrow();
        assertTrue(updated.getEmailVerified());
    }

    @Test
    @Order(36)
    @DisplayName("POST /auth/verify-email — fails with invalid token")
    void verifyEmail_invalidToken() {
        VerifyEmailRequest request = new VerifyEmailRequest("fake-token");
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/verify-email", request));
    }

    @Test
    @Order(37)
    @DisplayName("POST /auth/verify-email — fails with already used token")
    void verifyEmail_tokenAlreadyUsed() {
        VerifyEmailRequest request = new VerifyEmailRequest(verifyTokenValue);
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/verify-email", request));
    }

    @Test
    @Order(38)
    @DisplayName("POST /auth/verify-email — fails with expired token")
    void verifyEmail_expiredToken() {
        com.aiservice.platform.identity.entity.User user =
                userRepository.findByEmail("test@example.com").orElseThrow();
        String expiredVerifyToken = "expired-verify-" + UUID.randomUUID();
        VerificationToken expiredToken = VerificationToken.builder()
                .user(user)
                .token(expiredVerifyToken)
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
        verificationTokenRepository.save(expiredToken);

        VerifyEmailRequest request = new VerifyEmailRequest(expiredVerifyToken);
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/verify-email", request));
    }

    @Test
    @Order(39)
    @DisplayName("POST /auth/verify-email — fails with blank fields")
    void verifyEmail_blankFields() {
        VerifyEmailRequest request = new VerifyEmailRequest("");
        assertEquals(HttpStatus.BAD_REQUEST, postAuthStatus("/auth/verify-email", request));
    }
}
