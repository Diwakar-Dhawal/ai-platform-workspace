package com.aiservice.applications.insighttube.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Gateway filter that validates JWT access tokens issued by the Identity Service.
 *
 * <p>When applied to a route, it:
 * <ol>
 *   <li>Extracts the {@code Authorization: Bearer <token>} header</li>
 *   <li>Validates signature, expiration, and issuer</li>
 *   <li>Checks that the token type is {@code ACCESS}</li>
 *   <li>Forwards key claims as HTTP headers to downstream services</li>
 * </ol>
 *
 * <p>Configuration args:
 * <ul>
 *   <li>{@code secret} – HMAC signing key (must match the Identity Service)</li>
 *   <li>{@code issuer} – Expected JWT issuer (e.g. {@code identity-service})</li>
 * </ul>
 */
@Slf4j
@Component
public class JwtValidationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtValidationGatewayFilterFactory.Config> {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String CLAIM_USER_ID = "uid";
    private static final String CLAIM_USERNAME = "uname";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_SESSION_ID = "sid";
    private static final String HEADER_USER_ID = "X-User-Id";
    private static final String HEADER_USERNAME = "X-User-Username";
    private static final String HEADER_ROLES = "X-User-Roles";
    private static final String HEADER_CLIENT_ID = "X-Client-Id";
    private static final String HEADER_SESSION_ID = "X-Session-Id";

    public JwtValidationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {

            ServerHttpRequest request = exchange.getRequest();
            String authorizationHeader =
                    request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // ── No token → 401 ──
            if (authorizationHeader == null
                    || !authorizationHeader.startsWith(BEARER_PREFIX)) {
                log.debug("Missing or invalid Authorization header for {}",
                        request.getURI().getPath());
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authorizationHeader.substring(BEARER_PREFIX.length());

            try {
                SecretKey key = Keys.hmacShaKeyFor(
                        config.getSecret().getBytes(StandardCharsets.UTF_8));

                Claims claims = Jwts.parser()
                        .clockSkewSeconds(30)
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // ── Check issuer ──
                if (config.getIssuer() != null && !config.getIssuer().equals(claims.getIssuer())) {
                    log.debug("Invalid JWT issuer: expected={}, actual={}",
                            config.getIssuer(), claims.getIssuer());
                    return unauthorized(exchange, "Invalid token issuer");
                }

                // ── Check token type ──
                String tokenType = claims.get("typ", String.class);
                if (!"ACCESS".equals(tokenType)) {
                    log.debug("Invalid token type: expected=ACCESS, actual={}", tokenType);
                    return unauthorized(exchange, "Invalid token type");
                }

                // ── Forward claims as headers to downstream services ──
                String userId = claims.get(CLAIM_USER_ID, String.class);
                String username = claims.get(CLAIM_USERNAME, String.class);
                @SuppressWarnings("unchecked")
                List<String> roles = claims.get(CLAIM_ROLES, List.class);
                String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
                String clientId = claims.getAudience() != null
                        ? claims.getAudience().iterator().next()
                        : null;

                ServerHttpRequest mutatedRequest = request.mutate()
                        .header(HEADER_USER_ID, userId != null ? userId : "")
                        .header(HEADER_USERNAME, username != null ? username : "")
                        .header(HEADER_ROLES, roles != null ? String.join(",", roles) : "")
                        .header(HEADER_SESSION_ID, sessionId != null ? sessionId : "")
                        .header(HEADER_CLIENT_ID, clientId != null ? clientId : "")
                        .build();

                log.debug("JWT validated for user={} on path={}",
                        username, request.getURI().getPath());

                return chain.filter(exchange.mutate().request(mutatedRequest).build());

            } catch (JwtException e) {
                log.debug("JWT validation failed for {}: {}", request.getURI().getPath(),
                        e.getMessage());
                return unauthorized(exchange, "Invalid or expired token");
            } catch (Exception e) {
                log.error("Unexpected error during JWT validation", e);
                return unauthorized(exchange, "Token validation failed");
            }
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"status\":\"ERROR\",\"message\":\"%s\",\"data\":null}", message);
        DataBuffer buffer = response.bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));

        return response.writeWith(Mono.just(buffer));
    }

    /**
     * Configuration properties for the JWT validation filter.
     */
    public static class Config {
        private String secret;
        private String issuer;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }
    }
}
