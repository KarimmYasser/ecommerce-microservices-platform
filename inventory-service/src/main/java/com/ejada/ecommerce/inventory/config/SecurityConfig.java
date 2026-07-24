package com.ejada.ecommerce.inventory.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Public catalog reads, ROLE_ADMIN-only catalog writes, and unauthenticated
 * internal endpoints (protected by network topology instead — the gateway
 * never routes {@code /inventory/**} publicly, see
 * docs/infrastructure/api-gateway.md). JWTs are issued by wallet-service and
 * validated here with the shared secret; see
 * docs/security/authentication-authorization.md.
 */
@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
			"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
			"/actuator/health", "/actuator/info",
			"/inventory/**",
			// A real servlet container error-dispatches to /error before returning an
			// AccessDeniedException's 403 to the client. If /error itself required
			// ADMIN, that second pass through this chain would 401 and clobber the
			// original 403 — only reproduces with a live server, not @WebMvcTest.
			"/error"
	};

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// Disabling anonymous auth means "no token at all" has no Authentication
				// object, so it's an AuthenticationException (-> 401 via the entry point
				// below) rather than an AccessDeniedException (-> 403, reserved for a
				// valid token that simply lacks the required role).
				.anonymous(AbstractHttpConfigurer::disable)
				.exceptionHandling(handling -> handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**").permitAll()
						.anyRequest().hasRole("ADMIN"))
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

}
