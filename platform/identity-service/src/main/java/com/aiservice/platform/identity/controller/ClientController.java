package com.aiservice.platform.identity.controller;

import com.aiservice.platform.identity.dto.request.CreateClientRequest;
import com.aiservice.platform.identity.dto.request.UpdateClientRequest;
import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.ClientResponse;
import com.aiservice.platform.identity.service.ClientService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/admin/clients")
@PreAuthorize("hasRole('ADMIN')")
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ApiResponse<ClientResponse>> createClient(
            @Valid @RequestBody CreateClientRequest request) {

        ClientResponse response = clientService.createClient(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Client created successfully", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ClientResponse>>> getAllClients() {

        List<ClientResponse> clients = clientService.getAllClients();

        return ResponseEntity.ok(ApiResponse.success("Clients retrieved successfully", clients));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> getClientById(@PathVariable UUID id) {

        ClientResponse client = clientService.getClientById(id);

        return ResponseEntity.ok(ApiResponse.success("Client retrieved successfully", client));
    }

    @GetMapping("/by-client-id/{clientId}")
    public ResponseEntity<ApiResponse<ClientResponse>> getClientByClientId(@PathVariable String clientId) {

        ClientResponse client = clientService.getClientByClientId(clientId);

        return ResponseEntity.ok(ApiResponse.success("Client retrieved successfully", client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClientResponse>> updateClient(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClientRequest request) {

        ClientResponse client = clientService.updateClient(id, request);

        return ResponseEntity.ok(ApiResponse.success("Client updated successfully", client));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteClient(@PathVariable UUID id) {

        clientService.deleteClient(id);

        return ResponseEntity.ok(ApiResponse.success("Client deleted successfully", null));
    }
}
