package com.ejada.ecommerce.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejada.ecommerce.inventory.domain.Category;
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
class CategoryRepositoryTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("inventory_repo_test").withUsername("test").withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Autowired
	private CategoryRepository categoryRepository;

	@Test
	void existsBySlug_reflectsPersistedState() {
		assertThat(categoryRepository.existsBySlug("sneakers")).isFalse();

		categoryRepository.saveAndFlush(Category.builder().name("Sneakers").slug("sneakers").build());

		assertThat(categoryRepository.existsBySlug("sneakers")).isTrue();
	}

	@Test
	void save_duplicateSlug_violatesUniqueConstraint() {
		categoryRepository.saveAndFlush(Category.builder().name("Sneakers").slug("sneakers").build());

		assertThatThrownBy(() -> categoryRepository.saveAndFlush(
				Category.builder().name("Sneakers Again").slug("sneakers").build()))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void findBySlug_whenAbsent_returnsEmpty() {
		assertThat(categoryRepository.findBySlug("unknown")).isEmpty();
	}

}
