package com.ejada.ecommerce.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.inventory.dto.CategoryRequest;
import com.ejada.ecommerce.inventory.dto.CategoryResponse;
import com.ejada.ecommerce.inventory.dto.ImageInput;
import com.ejada.ecommerce.inventory.dto.ProductCreateRequest;
import com.ejada.ecommerce.inventory.dto.ProductDetailResponse;
import com.ejada.ecommerce.inventory.dto.VariantInput;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full HTTP-level lifecycle against a real MySQL (Testcontainers) — no
 * mocking, no WireMock. Proves catalog writes, catalog reads, and the
 * check/reserve/release cycle all work together end-to-end, plus that
 * concurrent reservations against the same stock never oversell (the
 * pessimistic-lock guarantee unit tests can't observe on a single thread).
 * See docs/implementation-plan/phase-1-inventory-service.md.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InventoryIntegrationTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("inventory_integration_test").withUsername("test").withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@LocalServerPort
	private int port;

	@Value("${jwt.secret}")
	private String jwtSecret;

	private TestRestTemplate restTemplate;
	private HttpHeaders adminHeaders;

	@BeforeEach
	void setUp() {
		restTemplate = new TestRestTemplate(new RestTemplateBuilder().rootUri("http://localhost:" + port));
		adminHeaders = new HttpHeaders();
		adminHeaders.setContentType(MediaType.APPLICATION_JSON);
		adminHeaders.setBearerAuth(mintToken("1", "ADMIN"));
	}

	/** Mints a token the same way wallet-service would — same shared secret, same claim shape. */
	private String mintToken(String subject, String role) {
		SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
		return Jwts.builder()
				.subject(subject)
				.claim("roles", List.of(role))
				.issuedAt(Date.from(Instant.now()))
				.expiration(Date.from(Instant.now().plus(Duration.ofHours(1))))
				.signWith(key)
				.compact();
	}

	private Long createCategory(String name, String slug) {
		var response = restTemplate.postForEntity("/api/v1/categories",
				new HttpEntity<>(new CategoryRequest(name, slug, null), adminHeaders), CategoryResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return response.getBody().id();
	}

	private ProductDetailResponse createProduct(Long categoryId, String sku, int initialQuantity) {
		var request = new ProductCreateRequest(
				"Integration Test Sneaker", "desc", "StepUp", categoryId,
				new BigDecimal("2999.00"), null, "INR", true,
				List.of(new ImageInput("https://img/1.png", 0)),
				List.of(new VariantInput(sku, "42", "Black", null, initialQuantity)));
		var response = restTemplate.postForEntity("/api/v1/products",
				new HttpEntity<>(request, adminHeaders), ProductDetailResponse.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return response.getBody();
	}

	@Test
	void fullLifecycle_create_list_reserve_release() {
		Long categoryId = createCategory("Integration Sneakers", "integration-sneakers-" + System.nanoTime());
		ProductDetailResponse product = createProduct(categoryId, "INT-SKU-" + System.nanoTime(), 10);
		Long variantId = product.variants().get(0).id();
		assertThat(product.variants().get(0).available()).isEqualTo(10);

		// Public list/detail reads, no auth required.
		var listResponse = restTemplate.getForEntity("/api/v1/products?categoryId=" + categoryId, Map.class);
		assertThat(listResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		// Reserve 4 units for order 500.
		var reserveResponse = restTemplate.postForEntity("/inventory/reserve",
				Map.of("orderId", 500, "items", List.of(Map.of("variantId", variantId, "quantity", 4))),
				Map.class);
		assertThat(reserveResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((Boolean) reserveResponse.getBody().get("reserved")).isTrue();

		ProductDetailResponse afterReserve = restTemplate.getForObject(
				"/api/v1/products/" + product.id(), ProductDetailResponse.class);
		assertThat(afterReserve.variants().get(0).available()).isEqualTo(6);

		// Idempotent replay: reserving again for the same order must not double-reserve.
		restTemplate.postForEntity("/inventory/reserve",
				Map.of("orderId", 500, "items", List.of(Map.of("variantId", variantId, "quantity", 4))),
				Map.class);
		ProductDetailResponse afterReplay = restTemplate.getForObject(
				"/api/v1/products/" + product.id(), ProductDetailResponse.class);
		assertThat(afterReplay.variants().get(0).available())
				.as("replaying reserve for the same orderId must not reserve twice")
				.isEqualTo(6);

		// Release gives the stock back.
		var releaseResponse = restTemplate.postForEntity("/inventory/release",
				Map.of("orderId", 500), Map.class);
		assertThat(releaseResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		ProductDetailResponse afterRelease = restTemplate.getForObject(
				"/api/v1/products/" + product.id(), ProductDetailResponse.class);
		assertThat(afterRelease.variants().get(0).available()).isEqualTo(10);

		// Idempotent replay: releasing an already-released order is a safe no-op.
		var releaseAgain = restTemplate.postForEntity("/inventory/release", Map.of("orderId", 500), Map.class);
		assertThat(releaseAgain.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void reserve_whenRequestExceedsAvailable_returns409AndReservesNothing() {
		Long categoryId = createCategory("Integration Scarce", "integration-scarce-" + System.nanoTime());
		ProductDetailResponse product = createProduct(categoryId, "INT-SCARCE-" + System.nanoTime(), 2);
		Long variantId = product.variants().get(0).id();

		var response = restTemplate.postForEntity("/inventory/reserve",
				Map.of("orderId", 600, "items", List.of(Map.of("variantId", variantId, "quantity", 5))),
				Map.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat((Boolean) response.getBody().get("reserved")).isFalse();

		ProductDetailResponse unchanged = restTemplate.getForObject(
				"/api/v1/products/" + product.id(), ProductDetailResponse.class);
		assertThat(unchanged.variants().get(0).available()).isEqualTo(2);
	}

	@Test
	void concurrentReserves_neverOversellBeyondOnHandStock() throws InterruptedException {
		Long categoryId = createCategory("Integration Concurrency", "integration-concurrency-" + System.nanoTime());
		ProductDetailResponse product = createProduct(categoryId, "INT-CONC-" + System.nanoTime(), 5);
		Long variantId = product.variants().get(0).id();

		int attempts = 10; // each requests 1 unit against only 5 available
		ExecutorService pool = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();

		for (int i = 0; i < attempts; i++) {
			long orderId = 1000 + i;
			pool.submit(() -> {
				ready.countDown();
				try {
					start.await();
					ResponseEntity<Map> response = restTemplate.postForEntity("/inventory/reserve",
							Map.of("orderId", orderId, "items", List.of(Map.of("variantId", variantId, "quantity", 1))),
							Map.class);
					if (response.getStatusCode() == HttpStatus.OK) {
						successCount.incrementAndGet();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			});
		}

		ready.await();
		start.countDown();
		pool.shutdown();
		assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

		assertThat(successCount.get()).as("exactly 5 of the 10 concurrent 1-unit reserves should succeed").isEqualTo(5);

		ProductDetailResponse afterAll = restTemplate.getForObject(
				"/api/v1/products/" + product.id(), ProductDetailResponse.class);
		assertThat(afterAll.variants().get(0).available())
				.as("stock must never go negative under concurrent load")
				.isZero();
	}

	@Test
	void adminWrite_withoutRole_isRejected() {
		var noAuthResponse = restTemplate.postForEntity("/api/v1/categories",
				new HttpEntity<>(new CategoryRequest("X", "x-" + System.nanoTime(), null)), Map.class);
		assertThat(noAuthResponse.getStatusCode().value()).isEqualTo(401);

		HttpHeaders userHeaders = new HttpHeaders();
		userHeaders.setContentType(MediaType.APPLICATION_JSON);
		userHeaders.setBearerAuth(mintToken("2", "USER"));
		var wrongRoleResponse = restTemplate.postForEntity("/api/v1/categories",
				new HttpEntity<>(new CategoryRequest("X", "x2-" + System.nanoTime(), null), userHeaders), Map.class);
		assertThat(wrongRoleResponse.getStatusCode().value()).isEqualTo(403);
	}

}
