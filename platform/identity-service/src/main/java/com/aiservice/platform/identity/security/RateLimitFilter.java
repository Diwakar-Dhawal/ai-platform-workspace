package com.aiservice.platform.identity.security;

import com.aiservice.platform.identity.config.RateLimitConfig;
import com.aiservice.platform.identity.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitConfig rateLimitConfig;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String clientIp = getClientIp(request);

        if (requestUri.contains("/auth/login")) {
            String key = "login:" + clientIp;
            if (!rateLimitService.isAllowed(key, rateLimitConfig.getLogin().getMaxAttempts(),
                    rateLimitConfig.getLogin().getWindowSeconds())) {
                rejectRequest(response, "Too many login attempts. Please try again later.");
                return;
            }
            rateLimitService.recordAttempt(key);
        } else if (requestUri.contains("/auth/register")) {
            String key = "register:" + clientIp;
            if (!rateLimitService.isAllowed(key, rateLimitConfig.getRegister().getMaxAttempts(),
                    rateLimitConfig.getRegister().getWindowSeconds())) {
                rejectRequest(response, "Too many registration attempts. Please try again later.");
                return;
            }
            rateLimitService.recordAttempt(key);
        } else if (requestUri.contains("/auth/refresh")) {
            String key = "refresh:" + clientIp;
            if (!rateLimitService.isAllowed(key, rateLimitConfig.getRefresh().getMaxAttempts(),
                    rateLimitConfig.getRefresh().getWindowSeconds())) {
                rejectRequest(response, "Too many refresh attempts. Please try again later.");
                return;
            }
            rateLimitService.recordAttempt(key);
        } else if (requestUri.contains("/auth/forgot-password")) {
            String key = "forgot-password:" + clientIp;
            if (!rateLimitService.isAllowed(key, rateLimitConfig.getForgotPassword().getMaxAttempts(),
                    rateLimitConfig.getForgotPassword().getWindowSeconds())) {
                rejectRequest(response, "Too many password reset attempts. Please try again later.");
                return;
            }
            rateLimitService.recordAttempt(key);
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void rejectRequest(HttpServletResponse response, String message) throws IOException {
        log.warn("Rate limit rejected: {}", message);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write(String.format(
                "{\"status\":\"FAILED\",\"message\":\"%s\",\"data\":null,\"error\":{\"code\":\"RATE_LIMIT_EXCEEDED\",\"message\":\"%s\"},\"warnings\":[]}",
                message, message
        ));
    }
}
