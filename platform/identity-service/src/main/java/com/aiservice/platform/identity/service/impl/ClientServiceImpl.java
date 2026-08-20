package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.dto.request.CreateClientRequest;
import com.aiservice.platform.identity.dto.request.UpdateClientRequest;
import com.aiservice.platform.identity.dto.response.ClientResponse;
import com.aiservice.platform.identity.entity.Client;
import com.aiservice.platform.identity.enums.ErrorCode;
import com.aiservice.platform.identity.exception.BadRequestException;
import com.aiservice.platform.identity.exception.DuplicateResourceException;
import com.aiservice.platform.identity.exception.UserNotFoundException;
import com.aiservice.platform.identity.mapper.ClientMapper;
import com.aiservice.platform.identity.repository.ClientRepository;
import com.aiservice.platform.identity.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepository;

    @Override
    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        log.info("Creating client: clientId={}, name={}", request.clientId(), request.name());

        if (clientRepository.existsByClientId(request.clientId())) {
            throw new DuplicateResourceException(ErrorCode.CLIENT_ALREADY_EXISTS, "Client with ID '" + request.clientId() + "' already exists");
        }

        Client client = Client.builder()
                .clientId(request.clientId())
                .name(request.name())
                .description(request.description())
                .build();

        Client savedClient = clientRepository.save(client);

        log.info("Client created successfully: id={}, clientId={}", savedClient.getId(), savedClient.getClientId());

        return ClientMapper.toResponse(savedClient);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        log.debug("Fetching all clients");

        return clientRepository.findAll()
                .stream()
                .map(ClientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientById(UUID id) {
        log.debug("Fetching client by ID: {}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Client not found with ID: " + id));

        return ClientMapper.toResponse(client);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse getClientByClientId(String clientId) {
        log.debug("Fetching client by clientId: {}", clientId);

        Client client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new UserNotFoundException("Client not found with clientId: " + clientId));

        return ClientMapper.toResponse(client);
    }

    @Override
    @Transactional
    public ClientResponse updateClient(UUID id, UpdateClientRequest request) {
        log.info("Updating client: id={}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Client not found with ID: " + id));

        if (request.name() != null) {
            client.setName(request.name());
        }

        if (request.description() != null) {
            client.setDescription(request.description());
        }

        Client updatedClient = clientRepository.save(client);

        log.info("Client updated successfully: id={}, clientId={}", updatedClient.getId(), updatedClient.getClientId());

        return ClientMapper.toResponse(updatedClient);
    }

    @Override
    @Transactional
    public void deleteClient(UUID id) {
        log.info("Deleting client: id={}", id);

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Client not found with ID: " + id));

        if (!client.getUserClientRoles().isEmpty()) {
            throw new BadRequestException(ErrorCode.CLIENT_IN_USE, "Cannot delete client with existing user-role assignments");
        }

        clientRepository.delete(client);

        log.info("Client deleted successfully: id={}, clientId={}", id, client.getClientId());
    }
}
