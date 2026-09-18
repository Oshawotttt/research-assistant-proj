package com.researchassistant.usermanagement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * The entire security surface. No custom OncePerRequestFilter: the Spring
 * Security resource server handles bearer-token validation.
 *
 * NOTE for anyone following a tutorial: Spring Boot 4 ships Spring Security 7.
 * Guides written for Security 6 show "new NimbusJwtEncoder(new ImmutableSecret)"
 * plus manual JwsHeader wiring. Security 7 added the withSecretKey builders used
 * below, which default to HS256, so no header plumbing is needed.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // Stateless bearer tokens, no cookies, so there is no CSRF vector.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Shared HMAC key. Storage Management, Research Evaluation and Updating build
     * the same key from the same secret to validate tokens without calling back.
     *
     * CONTRACTS.md: JWT_SECRET is "base64 of 32+ random bytes" and EVERY service
     * base64-decodes it before use. Do not change this to raw getBytes() - the
     * Python services decode, so raw bytes here would produce a different key and
     * every token we issue would fail their signature check. That is the exact
     * Java/Python bug CONTRACTS.md warns about.
     *
     * Generate one with:  openssl rand -base64 32
     */
    @Bean
    public SecretKey jwtSecretKey(@Value("${jwt.secret}") String secret) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "jwt.secret must be base64-encoded (see CONTRACTS.md). "
                            + "Generate one with: openssl rand -base64 32", ex);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "jwt.secret must decode to at least 32 bytes (256 bits) for HS256, but decoded to "
                            + bytes.length + " bytes. Generate one with: openssl rand -base64 32");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey).build();
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey).build();
    }
}
