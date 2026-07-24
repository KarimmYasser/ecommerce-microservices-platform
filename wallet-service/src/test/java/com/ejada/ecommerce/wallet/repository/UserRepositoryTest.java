package com.ejada.ecommerce.wallet.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejada.ecommerce.wallet.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
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
class UserRepositoryTest {

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

	private User user(String email) {
		return User.builder().email(email).passwordHash("hash").fullName("Ahmed").build();
	}

	@Test
	void existsByEmail_reflectsPersistedState() {
		assertThat(userRepository.existsByEmail("a@b.com")).isFalse();

		userRepository.saveAndFlush(user("a@b.com"));

		assertThat(userRepository.existsByEmail("a@b.com")).isTrue();
	}

	@Test
	void save_duplicateEmail_violatesUniqueConstraint() {
		userRepository.saveAndFlush(user("a@b.com"));

		assertThatThrownBy(() -> userRepository.saveAndFlush(user("a@b.com")))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findByEmail_whenAbsent_returnsEmpty() {
		assertThat(userRepository.findByEmail("unknown@b.com")).isEmpty();
	}

}
