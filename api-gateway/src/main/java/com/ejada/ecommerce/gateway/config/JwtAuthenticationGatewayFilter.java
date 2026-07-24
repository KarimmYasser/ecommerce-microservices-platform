package com.ejada.ecommerce.gateway.config;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationGatewayFilter implements GlobalFilter, Ordered {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtService jwtService;

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		ServerHttpRequest request = exchange.getRequest();
		String path = request.getPath().value();
		HttpMethod method = request.getMethod();

		if (isPublicPath(path, method)) {
			return chain.filter(exchange);
		}

		String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		String token = authHeader.substring(BEARER_PREFIX.length());
		var authenticatedUserOpt = jwtService.validate(token);
		if (authenticatedUserOpt.isEmpty()) {
			exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
			return exchange.getResponse().setComplete();
		}

		JwtService.AuthenticatedUser user = authenticatedUserOpt.get();
		ServerHttpRequest mutatedRequest = request.mutate()
				.header("X-User-Id", user.userId())
				.header("X-User-Roles", String.join(",", user.roles()))
				.build();

		return chain.filter(exchange.mutate().request(mutatedRequest).build());
	}

	@Override
	public int getOrder() {
		return -100;
	}

	private boolean isPublicPath(String path, HttpMethod method) {
		if (path.startsWith("/api/v1/auth/") ||
				path.startsWith("/swagger-ui") ||
				path.startsWith("/v3/api-docs") ||
				path.startsWith("/actuator/")) {
			return true;
		}

		if (HttpMethod.GET.equals(method) &&
				(path.startsWith("/api/v1/products") || path.startsWith("/api/v1/categories"))) {
			return true;
		}

		return false;
	}

}
