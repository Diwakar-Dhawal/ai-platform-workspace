package com.aiservice.platform.identity.service;

public interface EmailService {

    void sendVerificationEmail(String to, String username, String verificationUrl);

    void sendPasswordResetEmail(String to, String username, String resetUrl);
}
