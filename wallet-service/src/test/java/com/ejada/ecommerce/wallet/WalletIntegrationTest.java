package com.ejada.ecommerce.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.wallet.dto.LoginRequest;
import com.ejada.ecommerce.wallet.dto.LoginResponse;
import com.ejada.ecommerce.wallet.dto.RegisterRequest;
import com.ejada.ecommerce.wallet.dto.RegisterResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
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
 * mocking. Uses the real /auth/register + /auth/login endpoints to obtain a
 * genuine token (unlike inventory-service's integration test, which has to
 * mint one itself since it isn't the token issuer). See
 * docs/implementation-plan/phase-2-wallet-service.md.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WalletIntegrationTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("wallet_integration_test").withUsername("test").withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@LocalServerPort
	private int port;

	private TestRestTemplate restTemplate;

	private TestRestTemplate client() {
		if (restTemplate == null) {
			restTemplate = new TestRestTemplate(new RestTemplateBuilder().rootUri("http://localhost:" + port));
		}
		return restTemplate;
	}

	private record Registered(Long userId, String token) {
	}

	private Registered registerAndLogin(String email) {
		var registerResponse = client().postForEntity("/api/v1/auth/register",
				new RegisterRequest(email, "secret123", "Test User", null), RegisterResponse.class);
		assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		var loginResponse = client().postForEntity("/api/v1/auth/login",
				new LoginRequest(email, "secret123"), LoginResponse.class);
		assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		return new Registered(registerResponse.getBody().userId(), loginResponse.getBody().accessToken());
	}

	private HttpHeaders authHeaders(String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);
		return headers;
	}

	@Test
	void fullLifecycle_register_login_deposit_withdraw_transactions() {
		Registered reg = registerAndLogin("lifecycle-" + System.nanoTime() + "@b.com");
		HttpHeaders headers = authHeaders(reg.token());

		var profile = client().exchange("/api/v1/users/me", org.springframework.http.HttpMethod.GET,
				new HttpEntity<>(headers), Map.class);
		assertThat(profile.getStatusCode()).isEqualTo(HttpStatus.OK);

		var wallet = client().exchange("/api/v1/wallets/me", org.springframework.http.HttpMethod.GET,
				new HttpEntity<>(headers), Map.class);
		assertThat((Number) wallet.getBody().get("balance")).isEqualTo(0.0);

		var deposit = client().postForEntity("/api/v1/wallets/me/deposit",
				new HttpEntity<>(Map.of("amount", 100.00), headers), Map.class);
		assertThat(deposit.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(((Number) deposit.getBody().get("balance")).doubleValue()).isEqualTo(100.00);

		var withdraw = client().postForEntity("/api/v1/wallets/me/withdraw",
				new HttpEntity<>(Map.of("amount", 30.00), headers), Map.class);
		assertThat(((Number) withdraw.getBody().get("balance")).doubleValue()).isEqualTo(70.00);

		var transactions = client().exchange("/api/v1/wallets/me/transactions", org.springframework.http.HttpMethod.GET,
				new HttpEntity<>(headers), Map.class);
		assertThat(transactions.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat((List<?>) transactions.getBody().get("content")).hasSize(2);
	}

	@Test
	void internalDebitCredit_areIdempotentAndAdjustBalance() {
		Registered reg = registerAndLogin("internal-" + System.nanoTime() + "@b.com");
		HttpHeaders headers = authHeaders(reg.token());
		client().postForEntity("/api/v1/wallets/me/deposit", new HttpEntity<>(Map.of("amount", 100.00), headers), Map.class);

		String idemKey = "order-" + System.nanoTime();
		var debit1 = client().postForEntity("/wallets/" + reg.userId() + "/debit",
				Map.of("amount", 40.00, "currency", "USD", "idempotencyKey", idemKey), Map.class);
		assertThat(debit1.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(((Number) debit1.getBody().get("balanceAfter")).doubleValue()).isEqualTo(60.00);

		// Idempotent replay must not double-charge.
		var debit2 = client().postForEntity("/wallets/" + reg.userId() + "/debit",
				Map.of("amount", 40.00, "currency", "USD", "idempotencyKey", idemKey), Map.class);
		assertThat(((Number) debit2.getBody().get("balanceAfter")).doubleValue()).isEqualTo(60.00);

		String refundKey = "refund-" + System.nanoTime();
		var credit = client().postForEntity("/wallets/" + reg.userId() + "/credit",
				Map.of("amount", 40.00, "currency", "USD", "idempotencyKey", refundKey), Map.class);
		assertThat(((Number) credit.getBody().get("balanceAfter")).doubleValue()).isEqualTo(100.00);

		var balance = client().getForObject("/wallets/" + reg.userId() + "/balance", Map.class);
		assertThat(((Number) balance.get("balance")).doubleValue()).isEqualTo(100.00);
	}

	@Test
	void debit_whenInsufficientFunds_returns402AndDoesNotMutate() {
		Registered reg = registerAndLogin("scarce-" + System.nanoTime() + "@b.com");
		HttpHeaders headers = authHeaders(reg.token());
		client().postForEntity("/api/v1/wallets/me/deposit", new HttpEntity<>(Map.of("amount", 10.00), headers), Map.class);

		var response = client().postForEntity("/wallets/" + reg.userId() + "/debit",
				Map.of("amount", 999.00, "currency", "USD", "idempotencyKey", "order-x"), Map.class);

		assertThat(response.getStatusCode().value()).isEqualTo(402);

		var balance = client().getForObject("/wallets/" + reg.userId() + "/balance", Map.class);
		assertThat(((Number) balance.get("balance")).doubleValue()).isEqualTo(10.00);
	}

	@Test
	void register_duplicateEmail_returns409() {
		String email = "dup-" + System.nanoTime() + "@b.com";
		registerAndLogin(email);

		var second = client().postForEntity("/api/v1/auth/register",
				new RegisterRequest(email, "secret123", "Another", null), Map.class);

		assertThat(second.getStatusCode().value()).isEqualTo(409);
	}

	@Test
	void login_withWrongPassword_returns401() {
		String email = "wrongpw-" + System.nanoTime() + "@b.com";
		registerAndLogin(email);

		var response = client().postForEntity("/api/v1/auth/login",
				new LoginRequest(email, "totally-wrong"), Map.class);

		assertThat(response.getStatusCode().value()).isEqualTo(401);
	}

	@Test
	void protectedRoute_withoutToken_returns401() {
		var response = client().getForEntity("/api/v1/wallets/me", Map.class);

		assertThat(response.getStatusCode().value()).isEqualTo(401);
	}

	@Test
	void concurrentDebits_neverAllowBalanceToGoNegative() throws InterruptedException {
		Registered reg = registerAndLogin("concurrency-" + System.nanoTime() + "@b.com");
		HttpHeaders headers = authHeaders(reg.token());
		client().postForEntity("/api/v1/wallets/me/deposit", new HttpEntity<>(Map.of("amount", 100.00), headers), Map.class);

		int attempts = 5; // each debits 30 against only 100 available -> exactly 3 can succeed
		ExecutorService pool = Executors.newFixedThreadPool(attempts);
		CountDownLatch ready = new CountDownLatch(attempts);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger successCount = new AtomicInteger();

		for (int i = 0; i < attempts; i++) {
			String key = "concurrent-" + reg.userId() + "-" + i;
			pool.submit(() -> {
				ready.countDown();
				try {
					start.await();
					ResponseEntity<Map> response = client().postForEntity("/wallets/" + reg.userId() + "/debit",
							Map.of("amount", 30.00, "currency", "USD", "idempotencyKey", key), Map.class);
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

		assertThat(successCount.get()).as("exactly 3 of the 5 concurrent 30-unit debits should succeed against 100 available").isEqualTo(3);

		var balance = client().getForObject("/wallets/" + reg.userId() + "/balance", Map.class);
		assertThat(((Number) balance.get("balance")).doubleValue())
				.as("balance must never go negative under concurrent load")
				.isEqualTo(10.00);
	}

}
