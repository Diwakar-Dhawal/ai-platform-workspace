package com.aiservice.platform.identity.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/public")
    public String publicApi() {
        return "Public";
    }


    @GetMapping("/private")
    public String privateApi() {
        return "Private";
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/user")
    public String userApi() {
        return "User";
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/manager")
    public String managerApi() {
        return "Manager";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public String adminApi() {
        return "Admin";
    }

    @GetMapping("/me")
    public String me() {
        return "Authenticated";
    }
}
