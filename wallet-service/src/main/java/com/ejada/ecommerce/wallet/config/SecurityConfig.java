package com.ejada.ecommerce.wallet.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Public auth endpoints, everything else authenticated by JWT. wallet-service
 * both signs (JwtService.issue) and validates tokens — see
 * docs/security/authentication-authorization.md.
 */
@RequiredArgsConstructor
@EnableWebSecurity
@Configuration
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {
			"/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
			"/actuator/health", "/actuator/info",
			"/api/v1/auth/**",
			"/wallets/*/debit", "/wallets/*/credit", "/wallets/*/balance",
			// A real servlet container error-dispatches to /error before returning an
			// AccessDeniedException's 403 to the client. If /error itself required
			// auth, that second pass through this chain would 401 and clobber the
			// original 403 — see inventory-service's SecurityConfig for the full story.
			"/error"
	};

	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				// Disabling anonymous auth means "no token at all" has no Authentication
				// object, so it's an AuthenticationException (-> 401 via the entry point
				// below) rather than an AccessDeniedException (-> 403).
				.anonymous(AbstractHttpConfigurer::disable)
				.exceptionHandling(handling -> handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
		return http.build();
	}

}
