package com.ejada.ecommerce.shop;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.shop.domain.OrderStatus;
import com.ejada.ecommerce.shop.dto.OrderResponse;
import com.ejada.ecommerce.shop.repository.CartRepository;
import com.ejada.ecommerce.shop.repository.OrderRepository;
import com.ejada.ecommerce.shop.repository.ReviewRepository;
import com.ejada.ecommerce.shop.repository.WishlistItemRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SystemEndToEndTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("shop_e2e_test").withUsername("test").withPassword("test");

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

		String invUrl = "http://localhost:" + inventoryWireMock.port();
		String walUrl = "http://localhost:" + walletWireMock.port();
		registry.add("spring.cloud.openfeign.client.config.inventory-service.url", () -> invUrl);
		registry.add("spring.cloud.openfeign.client.config.wallet-service.url", () -> walUrl);
	}

	@AfterAll
	static void tearDown() {
		if (inventoryWireMock != null) inventoryWireMock.stop();
		if (walletWireMock != null) walletWireMock.stop();
	}

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private WishlistItemRepository wishlistItemRepository;

	@Autowired
	private OrderRepository orderRepository;

	@Autowired
	private ReviewRepository reviewRepository;

	@BeforeEach
	void resetState() {
		cartRepository.deleteAll();
		wishlistItemRepository.deleteAll();
		orderRepository.deleteAll();
		reviewRepository.deleteAll();
		inventoryWireMock.resetAll();
		walletWireMock.resetAll();
	}

	@Test
	@WithMockUser(username = "77")
	void completeCustomerJourney_happyPathAndCancellation() throws Exception {
		// 1. Add product to cart (including mandatory variantId)
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/cart/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": 10,
								  "variantId": 20,
								  "quantity": 2
								}
								"""))
				.andExpect(status().isOk());

		// 2. Add product to wishlist
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/wishlist/items")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "productId": 10
								}
								"""))
				.andExpect(status().isCreated());

		// 3. Stub inventory batch & reserve, and wallet debit
		inventoryWireMock.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlMatching("/api/v1/products/batch.*"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("""
								[
								  {
								    "id": 10,
								    "name": "E2E Phone",
								    "basePrice": 250.00,
								    "currency": "USD",
								    "primaryImageUrl": "http://img/phone.png",
								    "active": true
								  }
								]
								""")));

		inventoryWireMock.stubFor(post(urlEqualTo("/inventory/reserve"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"reserved\":true,\"shortfall\":[]}")));

		walletWireMock.stubFor(post(urlEqualTo("/wallets/77/debit"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"transactionId\":999,\"balanceAfter\":1500.00}")));

		// 4. Perform checkout saga
		MvcResult checkoutResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "shippingAddress": "456 E2E Highway"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		OrderResponse order = objectMapper.readValue(checkoutResult.getResponse().getContentAsString(), OrderResponse.class);
		assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);

		// Verify stock reserve and wallet debit calls were issued
		inventoryWireMock.verify(postRequestedFor(urlEqualTo("/inventory/reserve")));
		walletWireMock.verify(postRequestedFor(urlEqualTo("/wallets/77/debit")));

		// 5. Submit product review
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/products/10/reviews")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "rating": 5,
								  "title": "Fantastic Product",
								  "comment": "Loved the performance!"
								}
								"""))
				.andExpect(status().isCreated());

		// 6. Stub cancellation refund & release
		walletWireMock.stubFor(post(urlEqualTo("/wallets/77/credit"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"transactionId\":1000,\"balanceAfter\":2000.00}")));

		inventoryWireMock.stubFor(post(urlEqualTo("/inventory/release"))
				.willReturn(aResponse()
						.withStatus(200)
						.withHeader("Content-Type", "application/json")
						.withBody("{\"released\":true}")));

		// 7. Cancel order
		mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/orders/" + order.id() + "/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		// Verify credit refund and release calls
		walletWireMock.verify(postRequestedFor(urlEqualTo("/wallets/77/credit")));
		inventoryWireMock.verify(postRequestedFor(urlEqualTo("/inventory/release")));
	}

}
