package com.researchassistant.storage.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Scaffolding rules: Swagger UI is open, everything else is denied. Task 3
 * replaces this with the JWT filter that owns user tokens on /papers/** and
 * service tokens on /internal/**, so routes added before then will 403.
 */
@Configuration
class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers("/docs/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().denyAll())
				.build();
	}

}
