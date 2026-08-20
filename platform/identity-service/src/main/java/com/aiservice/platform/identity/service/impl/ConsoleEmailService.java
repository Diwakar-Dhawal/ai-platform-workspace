package com.aiservice.platform.identity.service.impl;

import com.aiservice.platform.identity.service.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Profile({"local", "test", "dev"})
public class ConsoleEmailService implements EmailService {

    @Override
    public void sendVerificationEmail(String to, String username, String verificationUrl) {
        log.info("""
                
                ╔══════════════════════════════════════════════════════════════╗
                ║                    📧 EMAIL VERIFICATION                     ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  To: {}                                                      
                ║  Subject: Verify your email address                          
                ║                                                              ║
                ║  Hi {},                                                     
                ║                                                              ║
                ║  Please verify your email address by clicking the link:      
                ║                                                              ║
                ║  {}                                                         
                ║                                                              ║
                ║  This link expires in 24 hours.                              
                ║                                                              ║
                ║  If you didn't create an account, please ignore this email.  
                ╚══════════════════════════════════════════════════════════════╝
                """,
                to, username, verificationUrl
        );
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetUrl) {
        log.info("""
                
                ╔══════════════════════════════════════════════════════════════╗
                ║                    🔑 PASSWORD RESET                         ║
                ╠══════════════════════════════════════════════════════════════╣
                ║  To: {}                                                      
                ║  Subject: Reset your password                                
                ║                                                              ║
                ║  Hi {},                                                     
                ║                                                              ║
                ║  You requested a password reset. Click the link below:       
                ║                                                              ║
                ║  {}                                                         
                ║                                                              ║
                ║  This link expires in 1 hour.                                
                ║                                                              ║
                ║  If you didn't request a reset, please ignore this email.    
                ╚══════════════════════════════════════════════════════════════╝
                """,
                to, username, resetUrl
        );
    }
}
