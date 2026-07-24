package com.ejada.ecommerce.shop;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.shop.domain.Cart;
import com.ejada.ecommerce.shop.domain.CartItem;
import com.ejada.ecommerce.shop.repository.CartRepository;
import com.ejada.ecommerce.shop.repository.OrderRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OrderIntegrationTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("shop_integration_test").withUsername("test").withPassword("test");

	static WireMockServer wireMockServer;

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
		if (wireMockServer != null && wireMockServer.isRunning()) {
			String baseUrl = "http://localhost:" + wireMockServer.port();
			registry.add("spring.cloud.openfeign.client.config.inventory-service.url", () -> baseUrl);
			registry.add("spring.cloud.openfeign.client.config.wallet-service.url", () -> baseUrl);
		}
	}

	@BeforeAll
	static void startWireMock() {
		wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
		wireMockServer.start();
	}

	@AfterAll
	static void stopWireMock() {
		if (wireMockServer != null) {
			wireMockServer.stop();
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private OrderRepository orderRepository;

	@BeforeEach
	void setUp() {
		wireMockServer.resetAll();
		orderRepository.deleteAll();
		cartRepository.deleteAll();
	}

	@Test
	@WithMockUser(username = "1")
	@DisplayName("Full checkout saga integration: stubbed Inventory & Wallet APIs over HTTP, verifies reservation & debit requests")
	void checkout_fullSaga_success() throws Exception {
		Cart cart = Cart.builder().userId(1L).build();
		CartItem item = CartItem.builder()
				.cart(cart)
				.productId(10L)
				.variantId(20L)
				.quantity(2)
				.unitPriceSnapshot(new BigDecimal("50.00"))
				.build();
		cart.addItem(item);
		cartRepository.saveAndFlush(cart);

		wireMockServer.stubFor(get(urlMatching("/api/v1/products/batch.*"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("[{\"id\":10,\"name\":\"Test Product\",\"basePrice\":50.00,\"currency\":\"USD\",\"primaryImageUrl\":\"img.jpg\",\"active\":true}]")));

		wireMockServer.stubFor(post(urlEqualTo("/inventory/reserve"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"reserved\":true,\"shortfall\":[]}")));

		wireMockServer.stubFor(post(urlEqualTo("/wallets/1/debit"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"transactionId\":999,\"balanceAfter\":100.00}")));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("CONFIRMED"))
				.andExpect(jsonPath("$.grandTotal").value(100.00))
				.andExpect(jsonPath("$.paymentTransactionId").value("999"));

		wireMockServer.verify(postRequestedFor(urlEqualTo("/inventory/reserve")));
		wireMockServer.verify(postRequestedFor(urlEqualTo("/wallets/1/debit")));
	}

	@Test
	@WithMockUser(username = "1")
	@DisplayName("Checkout stock shortfall integration: Inventory returns 409 -> 409 Conflict response, no wallet debit")
	void checkout_insufficientStock_returns409() throws Exception {
		Cart cart = Cart.builder().userId(1L).build();
		CartItem item = CartItem.builder()
				.cart(cart)
				.productId(10L)
				.variantId(20L)
				.quantity(5)
				.unitPriceSnapshot(new BigDecimal("50.00"))
				.build();
		cart.addItem(item);
		cartRepository.saveAndFlush(cart);

		wireMockServer.stubFor(get(urlMatching("/api/v1/products/batch.*"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("[{\"id\":10,\"name\":\"Test Product\",\"basePrice\":50.00,\"currency\":\"USD\",\"primaryImageUrl\":\"img.jpg\",\"active\":true}]")));

		wireMockServer.stubFor(post(urlEqualTo("/inventory/reserve"))
				.willReturn(aResponse()
						.withStatus(409)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"reserved\":false,\"shortfall\":[{\"variantId\":20,\"requested\":5,\"available\":2}]}")));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("OUT_OF_STOCK"));

		wireMockServer.verify(0, postRequestedFor(urlEqualTo("/wallets/1/debit")));
	}

	@Test
	@WithMockUser(username = "1")
	@DisplayName("Checkout insufficient funds integration: Wallet returns 402 -> 402 Payment Required response & releases stock")
	void checkout_insufficientFunds_returns402_andReleasesStock() throws Exception {
		Cart cart = Cart.builder().userId(1L).build();
		CartItem item = CartItem.builder()
				.cart(cart)
				.productId(10L)
				.variantId(20L)
				.quantity(2)
				.unitPriceSnapshot(new BigDecimal("50.00"))
				.build();
		cart.addItem(item);
		cartRepository.saveAndFlush(cart);

		wireMockServer.stubFor(get(urlMatching("/api/v1/products/batch.*"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("[{\"id\":10,\"name\":\"Test Product\",\"basePrice\":50.00,\"currency\":\"USD\",\"primaryImageUrl\":\"img.jpg\",\"active\":true}]")));

		wireMockServer.stubFor(post(urlEqualTo("/inventory/reserve"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"reserved\":true,\"shortfall\":[]}")));

		wireMockServer.stubFor(post(urlEqualTo("/wallets/1/debit"))
				.willReturn(aResponse()
						.withStatus(402)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"error\":\"INSUFFICIENT_FUNDS\"}")));

		wireMockServer.stubFor(post(urlEqualTo("/inventory/release"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("{\"released\":true}")));

		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders"))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.error").value("PAYMENT_FAILED"));

		wireMockServer.verify(postRequestedFor(urlEqualTo("/inventory/release")));
	}

}
