package com.ejada.ecommerce.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ejada.ecommerce.shop.domain.Review;
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
class ReviewRepositoryTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("shop_review_repo_test").withUsername("test").withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Autowired
	private ReviewRepository reviewRepository;

	@Test
	void existsByProductIdAndUserId_returnsTrueForSavedReview() {
		Review review = Review.builder()
				.productId(10L)
				.userId(1L)
				.authorNameSnapshot("Jane Doe")
				.rating(5)
				.title("Awesome")
				.body("Super good product!")
				.build();
		reviewRepository.saveAndFlush(review);

		assertThat(reviewRepository.existsByProductIdAndUserId(10L, 1L)).isTrue();
	}

	@Test
	void save_duplicateProductAndUser_violatesUniqueConstraint() {
		Review r1 = Review.builder().productId(10L).userId(1L).authorNameSnapshot("Jane").rating(5).build();
		Review r2 = Review.builder().productId(10L).userId(1L).authorNameSnapshot("Jane").rating(4).build();

		reviewRepository.saveAndFlush(r1);

		assertThatThrownBy(() -> reviewRepository.saveAndFlush(r2))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

}
