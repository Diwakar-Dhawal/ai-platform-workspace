package com.aiservice.platform.identity.integration;

import com.aiservice.platform.identity.dto.request.CreateClientRequest;
import com.aiservice.platform.identity.dto.request.LoginRequest;
import com.aiservice.platform.identity.dto.request.UpdateClientRequest;
import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.AuthResponse;
import com.aiservice.platform.identity.dto.response.ClientResponse;
import com.aiservice.platform.identity.repository.ClientRepository;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientIntegrationTest {

    @Autowired
    private TestDataSeeder seeder;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${local.server.port}")
    private int port;

    private RestTemplate restTemplate;

    private static String adminAccessToken;
    private static UUID createdClientId;

    private static final String CONTEXT = "/identity-service";
    private static final String ADMIN_CLIENT_ID = "admin-test-client";
    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "AdminP@ss1";

    @BeforeAll
    void setup() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new org.springframework.web.client.NoOpResponseErrorHandler());

        // Seed admin client and user
        seeder.seedClient(ADMIN_CLIENT_ID, "Admin Test Client");
        seeder.seedAdminUser(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_CLIENT_ID);

        // Login as admin to get access token
        adminAccessToken = loginAsAdmin();
    }

    private String loginAsAdmin() {
        LoginRequest loginRequest = new LoginRequest(ADMIN_EMAIL, ADMIN_PASSWORD, ADMIN_CLIENT_ID);
        HttpEntity<Object> entity = new HttpEntity<>(loginRequest, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/auth/login", entity, String.class);

        try {
            ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));
            return apiResponse.data().accessToken();
        } catch (Exception e) {
            throw new RuntimeException("Failed to login as admin", e);
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
    //  CREATE CLIENT TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("POST /admin/clients — success with valid data")
    void createClient_success() {
        CreateClientRequest request = new CreateClientRequest(
                "new-client", "New Client", "A new test client"
        );

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/clients", entity, String.class);

        assertEquals(HttpStatus.CREATED.value(), response.getStatusCode().value());

        try {
            ApiResponse<ClientResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ClientResponse.class));
            assertNotNull(apiResponse.data().id());
            assertEquals("new-client", apiResponse.data().clientId());
            assertEquals("New Client", apiResponse.data().name());
            createdClientId = apiResponse.data().id();
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    @Test
    @Order(2)
    @DisplayName("POST /admin/clients — fails with duplicate client ID")
    void createClient_duplicateClientId() {
        CreateClientRequest request = new CreateClientRequest(
                "new-client", "Duplicate Client", "Should fail"
        );

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/clients", entity, String.class);

        assertEquals(HttpStatus.CONFLICT.value(), response.getStatusCode().value());
    }

    @Test
    @Order(3)
    @DisplayName("POST /admin/clients — fails with invalid client ID format")
    void createClient_invalidClientId() {
        CreateClientRequest request = new CreateClientRequest(
                "client@#$!", "Invalid Client", "Should fail"
        );

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/clients", entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
    }

    @Test
    @Order(4)
    @DisplayName("POST /admin/clients — fails with blank fields")
    void createClient_blankFields() {
        CreateClientRequest request = new CreateClientRequest("", "", "");

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/clients", entity, String.class);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.getStatusCode().value());
    }

    // ─────────────────────────────────────────────
    //  GET CLIENTS TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(5)
    @DisplayName("GET /admin/clients — success")
    void getAllClients_success() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        try {
            ApiResponse<?> apiResponse = objectMapper.readValue(response.getBody(), ApiResponse.class);
            assertNotNull(apiResponse.data());
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    @Test
    @Order(6)
    @DisplayName("GET /admin/clients/{id} — success")
    void getClientById_success() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients/" + createdClientId, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        try {
            ApiResponse<ClientResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ClientResponse.class));
            assertEquals(createdClientId, apiResponse.data().id());
            assertEquals("new-client", apiResponse.data().clientId());
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    @Test
    @Order(7)
    @DisplayName("GET /admin/clients/{id} — fails with non-existent ID")
    void getClientById_notFound() {
        UUID fakeId = UUID.randomUUID();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients/" + fakeId, HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    @Test
    @Order(8)
    @DisplayName("GET /admin/clients/by-client-id/{clientId} — success")
    void getClientByClientId_success() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients/by-client-id/new-client", HttpMethod.GET, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        try {
            ApiResponse<ClientResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ClientResponse.class));
            assertEquals("new-client", apiResponse.data().clientId());
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    //  UPDATE CLIENT TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(9)
    @DisplayName("PUT /admin/clients/{id} — success")
    void updateClient_success() {
        UpdateClientRequest request = new UpdateClientRequest("Updated Client", "Updated description");

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients/" + createdClientId, HttpMethod.PUT, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        try {
            ApiResponse<ClientResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ClientResponse.class));
            assertEquals("Updated Client", apiResponse.data().name());
            assertEquals("Updated description", apiResponse.data().description());
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    @Test
    @Order(10)
    @DisplayName("PUT /admin/clients/{id} — partial update (name only)")
    void updateClient_partialUpdate() {
        UpdateClientRequest request = new UpdateClientRequest("Partial Update", null);

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients/" + createdClientId, HttpMethod.PUT, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        try {
            ApiResponse<ClientResponse> apiResponse = objectMapper.readValue(response.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, ClientResponse.class));
            assertEquals("Partial Update", apiResponse.data().name());
            assertEquals("Updated description", apiResponse.data().description()); // Unchanged
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }

    @Test
    @Order(11)
    @DisplayName("PUT /admin/clients/{id} — fails with non-existent ID")
    void updateClient_notFound() {
        UUID fakeId = UUID.randomUUID();
        UpdateClientRequest request = new UpdateClientRequest("Should Fail", null);

        HttpEntity<Object> entity = new HttpEntity<>(request, authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients/" + fakeId, HttpMethod.PUT, entity, String.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    // ─────────────────────────────────────────────
    //  DELETE CLIENT TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(12)
    @DisplayName("DELETE /admin/clients/{id} — success")
    void deleteClient_success() {
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients/" + createdClientId, HttpMethod.DELETE, entity, String.class);

        assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());

        // Verify deleted
        ResponseEntity<String> getResponse = restTemplate.exchange(
                baseUrl() + "/admin/clients/" + createdClientId, HttpMethod.GET, entity, String.class);
        assertEquals(HttpStatus.NOT_FOUND.value(), getResponse.getStatusCode().value());
    }

    @Test
    @Order(13)
    @DisplayName("DELETE /admin/clients/{id} — fails with non-existent ID")
    void deleteClient_notFound() {
        UUID fakeId = UUID.randomUUID();
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders());
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl() + "/admin/clients/" + fakeId, HttpMethod.DELETE, entity, String.class);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.getStatusCode().value());
    }

    // ─────────────────────────────────────────────
    //  AUTHORIZATION TESTS
    // ─────────────────────────────────────────────

    @Test
    @Order(14)
    @DisplayName("POST /admin/clients — fails without authentication")
    void createClient_unauthorized() {
        CreateClientRequest request = new CreateClientRequest(
                "unauth-client", "Unauthorized", "Should fail"
        );

        HttpEntity<Object> entity = new HttpEntity<>(request, jsonHeaders());
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl() + "/admin/clients", entity, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatusCode().value());
    }

    @Test
    @Order(15)
    @DisplayName("POST /admin/clients — fails with non-admin token")
    void createClient_forbidden() {
        // Register a non-admin user
        com.aiservice.platform.identity.dto.request.RegisterRequest registerReq =
                new com.aiservice.platform.identity.dto.request.RegisterRequest(
                        "regularuser", "regular@example.com", "RegularP@ss1", ADMIN_CLIENT_ID
                );
        HttpEntity<Object> regEntity = new HttpEntity<>(registerReq, jsonHeaders());
        restTemplate.postForEntity(baseUrl() + "/auth/register", regEntity, String.class);

        // Login as regular user
        LoginRequest loginReq = new LoginRequest("regular@example.com", "RegularP@ss1", ADMIN_CLIENT_ID);
        HttpEntity<Object> loginEntity = new HttpEntity<>(loginReq, jsonHeaders());
        ResponseEntity<String> loginResponse = restTemplate.postForEntity(
                baseUrl() + "/auth/login", loginEntity, String.class);

        try {
            ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(loginResponse.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(ApiResponse.class, AuthResponse.class));
            String userToken = apiResponse.data().accessToken();

            // Try to access admin endpoint with regular user token
            HttpHeaders headers = jsonHeaders();
            headers.setBearerAuth(userToken);
            CreateClientRequest request = new CreateClientRequest(
                    "forbidden-client", "Forbidden", "Should fail"
            );
            HttpEntity<Object> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    baseUrl() + "/admin/clients", entity, String.class);

            // Should be denied (401 or 403) - access is rejected
            int statusCode = response.getStatusCode().value();
            assertTrue(statusCode == 401 || statusCode == 403,
                    "Expected 401 or 403 but got " + statusCode);
        } catch (Exception e) {
            fail("Failed to parse response: " + e.getMessage());
        }
    }
}
