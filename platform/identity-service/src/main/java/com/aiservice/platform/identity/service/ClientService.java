package com.aiservice.platform.identity.service;

import com.aiservice.platform.identity.dto.request.CreateClientRequest;
import com.aiservice.platform.identity.dto.request.UpdateClientRequest;
import com.aiservice.platform.identity.dto.response.ClientResponse;

import java.util.List;
import java.util.UUID;

public interface ClientService {

    ClientResponse createClient(CreateClientRequest request);

    List<ClientResponse> getAllClients();

    ClientResponse getClientById(UUID id);

    ClientResponse getClientByClientId(String clientId);

    ClientResponse updateClient(UUID id, UpdateClientRequest request);

    void deleteClient(UUID id);
}
