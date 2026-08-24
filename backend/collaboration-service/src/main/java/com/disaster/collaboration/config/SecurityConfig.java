package com.disaster.collaboration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security configuration for Collaboration Service
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/collaboration/health").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/ws/**").permitAll()

                        /*
                         * Data subject rights routes require a verified identity.
                         *
                         * This must come before the permitAll below, because Spring
                         * Security applies the first matching rule. These endpoints
                         * export and destroy personal data and derive the subject from
                         * the authenticated principal; served anonymously they would
                         * let any caller erase any account's collaboration history.
                         */
                        .requestMatchers("/api/collaboration/privacy/**").authenticated()

                        /*
                         * TODO: the remaining routes are still unauthenticated, and
                         * they take the userId they act on from the request. That means
                         * GET /sessions/user/{userId} will return anyone's sessions to
                         * anyone who asks. Closing this needs the callers updated to
                         * send a bearer token, which is beyond the change that added
                         * the privacy routes -- but it is a live authorisation gap,
                         * not a stylistic one.
                         */
                        .requestMatchers("/api/collaboration/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:8080"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
