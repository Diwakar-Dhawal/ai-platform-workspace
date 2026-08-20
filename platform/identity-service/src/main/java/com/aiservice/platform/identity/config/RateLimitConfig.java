package com.aiservice.platform.identity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private Login login = new Login();
    private Register register = new Register();
    private Refresh refresh = new Refresh();
    private ForgotPassword forgotPassword = new ForgotPassword();

    @Data
    public static class Login {
        private int maxAttempts = 5;
        private int windowSeconds = 300; // 5 minutes
    }

    @Data
    public static class Register {
        private int maxAttempts = 3;
        private int windowSeconds = 3600; // 1 hour
    }

    @Data
    public static class Refresh {
        private int maxAttempts = 10;
        private int windowSeconds = 300; // 5 minutes
    }

    @Data
    public static class ForgotPassword {
        private int maxAttempts = 3;
        private int windowSeconds = 3600; // 1 hour
    }
}
