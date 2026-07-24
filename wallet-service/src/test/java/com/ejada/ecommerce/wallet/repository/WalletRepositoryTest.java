package com.ejada.ecommerce.wallet.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejada.ecommerce.wallet.domain.User;
import com.ejada.ecommerce.wallet.domain.Wallet;
import com.ejada.ecommerce.wallet.domain.WalletTransaction;
import com.ejada.ecommerce.wallet.domain.TransactionStatus;
import com.ejada.ecommerce.wallet.domain.TransactionType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
class WalletRepositoryTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("wallet_repo_test").withUsername("test").withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Autowired
	private UserRepository userRepository;
	@Autowired
	private WalletRepository walletRepository;
	@Autowired
	private WalletTransactionRepository transactionRepository;

	private User persistUser(String email) {
		return userRepository.saveAndFlush(User.builder().email(email).passwordHash("h").fullName("Ahmed").build());
	}

	@Test
	void findByUserId_returnsPersistedWallet() {
		User user = persistUser("a@b.com");
		walletRepository.saveAndFlush(Wallet.builder().user(user).balance(BigDecimal.TEN).currency("USD").build());

		var found = walletRepository.findByUserId(user.getId());

		assertThat(found).isPresent();
		assertThat(found.get().getBalance()).isEqualByComparingTo("10");
	}

	@Test
	void version_incrementsOnUpdate() {
		User user = persistUser("v@b.com");
		Wallet wallet = walletRepository.saveAndFlush(
				Wallet.builder().user(user).balance(BigDecimal.ZERO).currency("USD").build());
		long initialVersion = wallet.getVersion();

		wallet.setBalance(new BigDecimal("50.00"));
		walletRepository.saveAndFlush(wallet);

		assertThat(wallet.getVersion()).isGreaterThan(initialVersion);
	}

	@Test
	void transaction_duplicateIdempotencyKey_violatesUniqueConstraint() {
		User user = persistUser("t@b.com");
		Wallet wallet = walletRepository.saveAndFlush(
				Wallet.builder().user(user).balance(BigDecimal.ZERO).currency("USD").build());
		transactionRepository.saveAndFlush(ledgerRow(wallet, "order-1"));

		assertThatThrownBy(() -> transactionRepository.saveAndFlush(ledgerRow(wallet, "order-1")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findByWalletId_ordersNewestFirst() {
		User user = persistUser("p@b.com");
		Wallet wallet = walletRepository.saveAndFlush(
				Wallet.builder().user(user).balance(BigDecimal.ZERO).currency("USD").build());
		transactionRepository.saveAndFlush(ledgerRow(wallet, "k1"));
		transactionRepository.saveAndFlush(ledgerRow(wallet, "k2"));

		var page = transactionRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), PageRequest.of(0, 10));

		assertThat(page.getContent()).hasSize(2);
	}

	private WalletTransaction ledgerRow(Wallet wallet, String idempotencyKey) {
		return WalletTransaction.builder()
				.wallet(wallet).type(TransactionType.PAYMENT)
				.amount(BigDecimal.TEN).balanceAfter(BigDecimal.ZERO)
				.idempotencyKey(idempotencyKey).status(TransactionStatus.SUCCESS)
				.build();
	}

}
