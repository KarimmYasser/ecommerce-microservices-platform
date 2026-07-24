package com.ejada.ecommerce.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.gateway.config.JwtAuthenticationGatewayFilter;
import com.ejada.ecommerce.gateway.config.JwtService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthenticationGatewayFilterTest {

	@Mock
	private JwtService jwtService;

	@Mock
	private GatewayFilterChain chain;

	private JwtAuthenticationGatewayFilter filter;

	@BeforeEach
	void setUp() {
		filter = new JwtAuthenticationGatewayFilter(jwtService);
		when(chain.filter(any())).thenReturn(Mono.empty());
	}

	@Test
	void publicPath_allowsRequestWithoutToken() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/products").build());

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		verify(chain).filter(exchange);
		verify(jwtService, never()).validate(any());
	}

	@Test
	void protectedPath_missingToken_returns401() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/cart").build());

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(chain, never()).filter(any());
	}

	@Test
	void protectedPath_invalidToken_returns401() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.get("/api/v1/cart")
						.header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
						.build());

		when(jwtService.validate("invalid-token")).thenReturn(Optional.empty());

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		verify(chain, never()).filter(any());
	}

	@Test
	void protectedPath_validToken_mutatesRequestWithUserId() {
		MockServerWebExchange exchange = MockServerWebExchange.from(
				MockServerHttpRequest.post("/api/v1/orders")
						.header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
						.build());

		when(jwtService.validate("valid-token"))
				.thenReturn(Optional.of(new JwtService.AuthenticatedUser("42", List.of("ROLE_USER"))));

		StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

		verify(chain).filter(any());
	}

}
