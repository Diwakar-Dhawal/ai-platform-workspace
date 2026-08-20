package com.aiservice.platform.identity.mapper;

import com.aiservice.platform.identity.dto.response.ClientResponse;
import com.aiservice.platform.identity.entity.Client;

public final class ClientMapper {

    private ClientMapper() {
    }

    public static ClientResponse toResponse(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getClientId(),
                client.getName(),
                client.getDescription(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}
