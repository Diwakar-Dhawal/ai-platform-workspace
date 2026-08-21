package com.aiservice.platform.aiplatform.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Validates service-to-service calls using a shared secret.
 * Tube Service sends X-Service-Secret header on internal API calls.
 * 
 * On successful validation, sets a SERVICE authentication in SecurityContext
 * so that anyRequest().authenticated() passes.
 * 
 * If the secret is not configured, this filter is disabled (allows all requests).
 */
@Slf4j
@Component
public class ServiceAuthFilter extends OncePerRequestFilter {

    private static final String SECRET_HEADER = "X-Service-Secret";

    @Value("${service.secret:}")
    private String expectedSecret;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // If no secret is configured, skip validation (dev mode)
        if (expectedSecret == null || expectedSecret.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // Already authenticated (e.g., by JWT filter with a user token)
        if (SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String providedSecret = request.getHeader(SECRET_HEADER);

        if (providedSecret == null || !expectedSecret.equals(providedSecret)) {
            log.warn("Service auth failed: missing or invalid {} header from {}",
                    SECRET_HEADER, request.getRemoteAddr());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Service authentication required\"}");
            return;
        }

        // Set SERVICE authentication so anyRequest().authenticated() passes
        UsernamePasswordAuthenticationToken serviceAuth =
                new UsernamePasswordAuthenticationToken(
                        "service:internal", null,
                        Set.of(new SimpleGrantedAuthority("ROLE_SERVICE"))
                );
        serviceAuth.setDetails(
                new WebAuthenticationDetailsSource().buildDetails(request)
        );
        SecurityContextHolder.getContext().setAuthentication(serviceAuth);

        filterChain.doFilter(request, response);
    }
}
