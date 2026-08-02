package com.ejada.ecommerce.shop;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.shop.client.ResilientInventoryClient;
import com.ejada.ecommerce.shop.client.ResilientWalletClient;
import com.ejada.ecommerce.shop.client.dto.CreditRequest;
import com.ejada.ecommerce.shop.client.dto.DebitRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReleaseRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReserveRequest;
import com.ejada.ecommerce.shop.repository.CartRepository;
import com.ejada.ecommerce.shop.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ResilienceCircuitBreakerTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("shop_resilience_test")
			.withUsername("test")
			.withPassword("test");

	private static WireMockServer inventoryWireMock;
	private static WireMockServer walletWireMock;

	@DynamicPropertySource
	static void overrideProps(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);

		inventoryWireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
		inventoryWireMock.start();

		walletWireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
		walletWireMock.start();

		registry.add("spring.cloud.openfeign.client.config.inventory-service.url", () -> "http://localhost:" + inventoryWireMock.port());
		registry.add("spring.cloud.openfeign.client.config.wallet-service.url", () -> "http://localhost:" + walletWireMock.port());
	}

	@AfterAll
	static void tearDown() {
		if (inventoryWireMock != null) inventoryWireMock.stop();
		if (walletWireMock != null) walletWireMock.stop();
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ResilientInventoryClient resilientInventoryClient;

	@Autowired
	private ResilientWalletClient resilientWalletClient;

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private OrderRepository orderRepository;

	@BeforeEach
	void resetState() {
		cartRepository.deleteAll();
		orderRepository.deleteAll();
		inventoryWireMock.resetAll();
		walletWireMock.resetAll();

		// Reset circuit breakers to CLOSED state
		circuitBreakerRegistry.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
	}

	@Test
	@DisplayName("inventoryReserve circuit breaker opens on repeated 503 server errors and fails fast")
	void inventoryReserve_circuitBreakerOpensOnOutage() {
		inventoryWireMock.stubFor(post(urlEqualTo("/inventory/reserve"))
				.willReturn(aResponse().withStatus(503)));

		CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("inventoryReserve");
		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

		InventoryReserveRequest req = new InventoryReserveRequest(100L, List.of());

		for (int i = 0; i < 10; i++) {
			try {
				resilientInventoryClient.reserve(req);
			} catch (Exception ignored) {
			}
		}

		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

		// Subsequent call fails fast with CallNotPermittedException
		assertThatThrownBy(() -> resilientInventoryClient.reserve(req))
				.isInstanceOf(CallNotPermittedException.class);
	}

	@Test
	@DisplayName("inventoryReserve circuit breaker ignores 409 Conflict business exception (out of stock)")
	void inventoryReserve_ignoresBusinessConflict409() {
		inventoryWireMock.stubFor(post(urlEqualTo("/inventory/reserve"))
				.willReturn(aResponse()
						.withStatus(409)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"reserved\":false}")));

		CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("inventoryReserve");
		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

		InventoryReserveRequest req = new InventoryReserveRequest(100L, List.of());

		for (int i = 0; i < 5; i++) {
			try {
				resilientInventoryClient.reserve(req);
			} catch (Exception ignored) {
			}
		}

		// Breaker remains CLOSED because 409 is ignored from failure count
		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
		assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
	}

	@Test
	@DisplayName("walletDebit circuit breaker opens on repeated 500/503 errors")
	void walletDebit_circuitBreakerOpensOnOutage() {
		walletWireMock.stubFor(post(urlEqualTo("/wallets/1/debit"))
				.willReturn(aResponse().withStatus(503)));

		CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("walletDebit");
		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

		DebitRequest req = new DebitRequest(new BigDecimal("10.00"), "USD", "test-key");

		for (int i = 0; i < 10; i++) {
			try {
				resilientWalletClient.debit(1L, req);
			} catch (Exception ignored) {
			}
		}

		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

		assertThatThrownBy(() -> resilientWalletClient.debit(1L, req))
				.isInstanceOf(CallNotPermittedException.class);
	}

	@Test
	@DisplayName("walletDebit circuit breaker ignores 402 Payment Required business exception (insufficient funds)")
	void walletDebit_ignoresBusinessPayment402() {
		walletWireMock.stubFor(post(urlEqualTo("/wallets/1/debit"))
				.willReturn(aResponse()
						.withStatus(402)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"error\":\"INSUFFICIENT_FUNDS\"}")));

		CircuitBreaker breaker = circuitBreakerRegistry.circuitBreaker("walletDebit");
		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

		DebitRequest req = new DebitRequest(new BigDecimal("10.00"), "USD", "test-key");

		for (int i = 0; i < 5; i++) {
			try {
				resilientWalletClient.debit(1L, req);
			} catch (Exception ignored) {
			}
		}

		assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
		assertThat(breaker.getMetrics().getNumberOfFailedCalls()).isZero();
	}

	@Test
	@DisplayName("inventoryRelease retry attempts up to 3 times on transient failure")
	void inventoryRelease_retriesOnTransientFailure() {
		inventoryWireMock.stubFor(post(urlEqualTo("/inventory/release"))
				.willReturn(aResponse().withStatus(500)));

		try {
			resilientInventoryClient.release(new InventoryReleaseRequest(100L));
		} catch (Exception ignored) {
		}

		// Verify WireMock received 3 requests (initial attempt + 2 retries = 3 attempts)
		inventoryWireMock.verify(3, postRequestedFor(urlEqualTo("/inventory/release")));
	}

	@Test
	@DisplayName("walletCredit retry attempts up to 3 times on transient failure")
	void walletCredit_retriesOnTransientFailure() {
		walletWireMock.stubFor(post(urlEqualTo("/wallets/1/credit"))
				.willReturn(aResponse().withStatus(500)));

		try {
			resilientWalletClient.credit(1L, new CreditRequest(new BigDecimal("50.00"), "USD", "refund-key"));
		} catch (Exception ignored) {
		}

		walletWireMock.verify(3, postRequestedFor(urlEqualTo("/wallets/1/credit")));
	}

	@Test
	@WithMockUser(username = "1")
	@DisplayName("Actuator endpoints expose circuit breaker status and event history")
	void actuatorEndpoints_exposeCircuitBreakersAndEvents() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/circuitbreakers"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.circuitBreakers").exists());

		mockMvc.perform(MockMvcRequestBuilders.get("/actuator/circuitbreakerevents"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.circuitBreakerEvents").exists());
	}

}
