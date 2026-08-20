package com.aiservice.platform.identity.integration;

import com.aiservice.platform.identity.dto.request.AssignRolesRequest;
import com.aiservice.platform.identity.dto.request.LoginRequest;
import com.aiservice.platform.identity.dto.request.RegisterRequest;
import com.aiservice.platform.identity.dto.request.RemoveRolesRequest;
import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.dto.response.UserResponse;
import com.aiservice.platform.identity.repository.UserRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RoleIntegrationTest {

    @Autowired
    private TestDataSeeder seeder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate;

    private static String adminAccessToken;
    private static UUID testUserId;

    private static final String CONTEXT = "/identity-service";
    private static final String TEST_CLIENT_ID = "test-client";
    private static final String ADMIN_EMAIL = "roleadmin@example.com";
    private static final String ADMIN_PASSWORD = "AdminP@ss1";
    private static final String TEST_USER_EMAIL = "testuser@example.com";
    private static final String TEST_USER_PASSWORD = "UserP@ss1";

    @BeforeAll
    void setup() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new org.springframework.web.client.NoOpResponseErrorHandler());

        // Seed test data
        seeder.seedAll();

        // Create admin user with ADMIN role
        seeder.seedAdminUser(ADMIN_EMAIL, ADMIN_PASSWORD, TEST_CLIENT_ID);

        // Login as admin
        adminAccessToken = loginAs(ADMIN_EMAIL, ADMIN_PASSWORD);

        // Register a test user (will have USER role)
        registerTestUser();
    }

    private void registerTestUser() {
        RegisterRequest request = new RegisterRequest(
                "testroleuser", TEST_USER_EMAIL, TEST_USER_PASSWORD, TEST_CLIENT_ID
        );
        HttpEntity<Object> entity = new HttpEntity<>(request, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/auth/register", entity, String.class);

        try {
            ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));
            testUserId = apiResponse.data().user().id();
        } catch (Exception e) {
            throw new RuntimeException("Failed to register test user", e);
        }
    }

    private String loginAs(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password, TEST_CLIENT_ID);
        HttpEntity<Object> entity = new HttpEntity<>(loginRequest, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/auth/login", entity, String.class);

        try {
            ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));
            return apiResponse.data().accessToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to login", e);
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(adminAccessToken);
        return headers;
    }

    private String baseUrl() {
        return "http://localhost:" + port + CONTEXT;
    }

    // ─────────────────────────────────────────────
    //  ASSIGN ROLES TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /admin/users/{id}/clients/{clientId}/roles — success")
    void assignRoles_success() {
        AssignRolesRequest request = new AssignRolesRequest(List.of("ADMIN"));

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/users/" + testUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        try {
            ApiResponse<UserResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, UserResponse.class));
            assertTrue(apiResponse.data().roles().contains("ADMIN"));
            assertTrue(apiResponse.data().roles().contains("USER"));
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("POST /admin/users/{id}/clients/{clientId}/roles — idempotent (already assigned)")
    void assignRoles_idempotent() {
        AssignRolesRequest request = new AssignRolesRequest(List.of("ADMIN"));

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/users/" + testUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    }

    @Test
    @Order(3)
    @DisplayName("POST /admin/users/{id}/clients/{clientId}/roles — fails with non-existent role")
    void assignRoles_roleNotFound() {
        AssignRolesRequest request = new AssignRolesRequest(List.of("NONEXISTENT_ROLE"));

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/users/" + testUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
    }

    @Test
    @Order(4)
    @DisplayName("POST /admin/users/{id}/clients/{clientId}/roles — fails with non-existent user")
    void assignRoles_userNotFound() {
        UUID fakeUserId = UUID.randomUUID();
        AssignRolesRequest request = new AssignRolesRequest(List.of("ADMIN"));

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/users/" + fakeUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                entity, String.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    @Order(5)
    @DisplayName("POST /admin/users/{id}/clients/{clientId}/roles — fails with non-existent client")
    void assignRoles_clientNotFound() {
        AssignRolesRequest request = new AssignRolesRequest(List.of("ADMIN"));

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/users/" + testUserId + "/clients/nonexistent-client/roles",
                entity, String.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    // ─────────────────────────────────────────────
    //  GET USER ROLES TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(6)
    @DisplayName("GET /admin/users/{id}/clients/{clientId}/roles — success")
    void getUserRoles_success() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/users/" + testUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        try {
            ApiResponse<?> apiResponse = objectMapper.readValue(response.getBody(), ApiResponse.class);
            assertNotNull(apiResponse.data());
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  REMOVE ROLES TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(7)
    @DisplayName("DELETE /admin/users/{id}/clients/{clientId}/roles — success")
    void removeRoles_success() {
        RemoveRolesRequest request = new RemoveRolesRequest(List.of("ADMIN"));

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/users/" + testUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                HttpMethod.DELETE, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        try {
            ApiResponse<UserResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, UserResponse.class));
            assertFalse(apiResponse.data().roles().contains("ADMIN"));
            assertTrue(apiResponse.data().roles().contains("USER")); // USER role still assigned
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    @Test
    @Order(8)
    @DisplayName("DELETE /admin/users/{id}/clients/{clientId}/roles — idempotent (not assigned)")
    void removeRoles_idempotent() {
        RemoveRolesRequest request = new RemoveRolesRequest(List.of("ADMIN"));

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/users/" + testUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                HttpMethod.DELETE, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    }

    // ─────────────────────────────────────────────
    //  AUTHORIZATION TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("POST /admin/users/{id}/clients/{clientId}/roles — fails without authentication")
    void assignRoles_unauthorized() {
        AssignRolesRequest request = new AssignRolesRequest(List.of("ADMIN"));

        HttpEntity<Object> entity = new HttpEntity<>(request, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/users/" + testUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
    }

    @Test
    @Order(10)
    @DisplayName("POST /admin/users/{id}/clients/{clientId}/roles — fails with non-admin token")
    void assignRoles_forbidden() {
        // Register a non-admin user
        RegisterRequest registerReq = new RegisterRequest(
                "regularroleuser", "regularrole@example.com", "RegularP@ss1", TEST_CLIENT_ID
        );
        HttpEntity<Object> regEntity = new HttpEntity<>(registerReq, jsonHeaders());
        restTemplate.postForEntity(baseUrl() + "/auth/register", regEntity, String.class);

        // Login as regular user
        String userToken = loginAs("regularrole@example.com", "RegularP@ss1");

        // Try to assign roles with regular user token
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(userToken);
        AssignRolesRequest request = new AssignRolesRequest(List.of("ADMIN"));
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/users/" + testUserId + "/clients/" + TEST_CLIENT_ID + "/roles",
                entity, String.class);

        // Should be denied (401 or 403)
        int statusCode = response.getStatusCode().value();
        assertTrue(statusCode == 401 || statusCode == 403,
                "Expected 401 or 403 but got " + statusCode);
    }
}
