package com.ejada.ecommerce.inventory.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <token>}, validates it, and — if valid —
 * populates the SecurityContext so downstream {@code hasRole(...)} checks
 * work. An invalid/absent token simply leaves the request unauthenticated;
 * Spring Security's own rules (SecurityConfig) then decide 401 vs 403 vs allow.
 */
@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER_PREFIX)) {
			String token = header.substring(BEARER_PREFIX.length());
			jwtService.validate(token).ifPresent(user -> {
				List<GrantedAuthority> authorities = user.roles().stream()
						.map(role -> new SimpleGrantedAuthority("ROLE_" + role))
						.map(GrantedAuthority.class::cast)
						.toList();
				var authentication = new UsernamePasswordAuthenticationToken(user.userId(), null, authorities);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			});
		}
		chain.doFilter(request, response);
	}

}
