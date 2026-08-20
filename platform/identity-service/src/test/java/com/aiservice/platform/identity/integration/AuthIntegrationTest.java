package com.aiservice.platform.identity.integration;

import com.aiservice.platform.identity.dto.request.LoginRequest;
import com.aiservice.platform.identity.dto.request.RefreshTokenRequest;
import com.aiservice.platform.identity.dto.request.RegisterRequest;
import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.repository.RefreshTokenRepository;
import com.aiservice.platform.identity.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

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
    private ObjectMapper objectMapper;

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate;

    private static String registeredAccessToken;
    private static String registeredRefreshToken;

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
}
