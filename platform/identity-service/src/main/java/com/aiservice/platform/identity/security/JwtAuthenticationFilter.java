package com.aiservice.platform.identity.security;

import com.aiservice.platform.identity.enums.TokenType;
import com.aiservice.platform.identity.exception.UnauthorizedException;
import com.aiservice.platform.identity.service.CustomUserDetailsService;
import com.aiservice.platform.identity.service.JwtService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null
                || !authorizationHeader.startsWith(BEARER_PREFIX)) {

            filterChain.doFilter(request, response);
            return;
        }

        String token =
                authorizationHeader.substring(BEARER_PREFIX.length());

        try {

            // validates signature + expiration
            jwtService.extractClaims(token);

            if (jwtService.extractTokenType(token) != TokenType.ACCESS) {
                throw new JwtException("Invalid token type");
            }

            String username =
                    jwtService.extractUsername(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                CustomUserDetails userDetails =
                        (CustomUserDetails)
                                customUserDetailsService.loadUserByUsername(username);

                if (!jwtService.extractTokenVersion(token)
                        .equals(userDetails.getTokenVersion())) {

                    throw new JwtException("Token version mismatch");
                }

                Set<SimpleGrantedAuthority> authorities =
                        jwtService.extractRoles(token)
                                .stream()
                                .map(role ->
                                        new SimpleGrantedAuthority(
                                                "ROLE_" + role.name()
                                        ))
                                .collect(Collectors.toSet());

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder
                        .getContext()
                        .setAuthentication(authentication);
            }

        } catch (JwtException | IllegalArgumentException ex) {

            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}