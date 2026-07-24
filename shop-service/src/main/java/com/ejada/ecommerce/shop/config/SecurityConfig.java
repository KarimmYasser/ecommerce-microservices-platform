package com.ejada.ecommerce.shop.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Placeholder security posture for Phase 0: unlocks Swagger UI, the OpenAPI
 * document, and actuator health/info; everything else defaults to
 * authenticated. Real JWT validation lands in Phase 3 — see
 * docs/security/authentication-authorization.md and
 * docs/implementation-plan/phase-3-shop-service.md.
 */
@EnableWebSecurity
@Configuration
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
			"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
			"/actuator/health", "/actuator/info"
	};

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.anyRequest().authenticated());
		return http.build();
	}

}
