package com.disaster.collaboration.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Establishes the caller's identity from a bearer token.
 *
 * <p>This service previously had none: {@code SecurityConfig} permitted every route and
 * endpoints took the {@code userId} they operated on straight from the request. That is
 * workable for read-only demo traffic, but it cannot support data subject rights --
 * an erasure endpoint that trusts a caller-supplied identifier is a button for deleting
 * other people's data.
 *
 * <p>The filter never rejects a request on its own. It populates the security context
 * when a valid token is present and leaves it empty otherwise, letting
 * {@link SecurityConfig} decide which routes actually require authentication. That
 * keeps the existing permissive routes working while the privacy routes demand a real
 * identity.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null) {
                Claims claims = parseClaims(token);
                if (claims != null) {
                    List<String> roles = extractRoles(claims);
                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    claims.getSubject(), null, authorities));
                }
            }
        } catch (Exception e) {
            // A bad token means an anonymous request, not a server error. The message
            // is logged without the token itself, which would otherwise put a
            // credential into the log.
            log.debug("Bearer token could not be validated: {}", e.getClass().getSimpleName());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    /** Parses and verifies the token, returning its claims, or null when invalid. */
    private Claims parseClaims(String token) {
        try {
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Claims claims) {
        Object roles = claims.get("roles");
        return roles instanceof List ? (List<String>) roles : Collections.emptyList();
    }
}
