package com.aiservice.platform.identity.controller;

import com.aiservice.platform.identity.dto.request.AssignRolesRequest;
import com.aiservice.platform.identity.dto.request.RemoveRolesRequest;
import com.aiservice.platform.identity.dto.response.ApiResponse;
import com.aiservice.platform.identity.dto.response.UserResponse;
import com.aiservice.platform.identity.service.RoleService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class RoleController {

    private final RoleService roleService;

    @PostMapping("/{userId}/clients/{clientId}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> assignRoles(
            @PathVariable UUID userId,
            @PathVariable String clientId,
            @Valid @RequestBody AssignRolesRequest request) {

        UserResponse response = roleService.assignRoles(userId, clientId, request);

        return ResponseEntity.ok(ApiResponse.success("Roles assigned successfully", response));
    }

    @DeleteMapping("/{userId}/clients/{clientId}/roles")
    public ResponseEntity<ApiResponse<UserResponse>> removeRoles(
            @PathVariable UUID userId,
            @PathVariable String clientId,
            @Valid @RequestBody RemoveRolesRequest request) {

        UserResponse response = roleService.removeRoles(userId, clientId, request);

        return ResponseEntity.ok(ApiResponse.success("Roles removed successfully", response));
    }

    @GetMapping("/{userId}/clients/{clientId}/roles")
    public ResponseEntity<ApiResponse<List<String>>> getUserRoles(
            @PathVariable UUID userId,
            @PathVariable String clientId) {

        List<String> roles = roleService.getUserRoles(userId, clientId);

        return ResponseEntity.ok(ApiResponse.success("Roles retrieved successfully", roles));
    }
}
