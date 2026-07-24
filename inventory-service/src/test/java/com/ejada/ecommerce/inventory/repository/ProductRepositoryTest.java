package com.ejada.ecommerce.inventory.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.domain.Product;
import com.ejada.ecommerce.inventory.repository.spec.ProductSpecifications;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
class ProductRepositoryTest {

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
	private ProductRepository productRepository;

	@Autowired
	private CategoryRepository categoryRepository;

	private Category sneakers;
	private Category dresses;

	@BeforeEach
	void seed() {
		sneakers = categoryRepository.saveAndFlush(Category.builder().name("Sneakers").slug("sneakers").build());
		dresses = categoryRepository.saveAndFlush(Category.builder().name("Dresses").slug("dresses").build());

		productRepository.saveAndFlush(activeProduct("Slick Formal Sneaker", sneakers,
				new BigDecimal("2999.00"), new BigDecimal("4999.00"), true));
		productRepository.saveAndFlush(activeProduct("Trendy StepUp Pro", sneakers,
				new BigDecimal("3999.00"), null, false));
		productRepository.saveAndFlush(activeProduct("Elegant Wrap Dress", dresses,
				new BigDecimal("3800.00"), null, true));
		Product inactive = activeProduct("Discontinued Dress", dresses, new BigDecimal("1000.00"), null, false);
		inactive.setActive(false);
		productRepository.saveAndFlush(inactive);
	}

	private Product activeProduct(String name, Category category, BigDecimal basePrice, BigDecimal compareAtPrice, boolean isNew) {
		return Product.builder()
				.name(name).description(name + " description").category(category)
				.basePrice(basePrice).compareAtPrice(compareAtPrice).currency("INR").isNew(isNew).isActive(true)
				.build();
	}

	private Specification<Product> activeSpec() {
		return ProductSpecifications.isActive();
	}

	@Test
	void search_byQ_matchesNameCaseInsensitive() {
		var spec = ProductSpecifications.allOf(activeSpec(), ProductSpecifications.nameOrDescriptionContains("sneaker"));

		Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Slick Formal Sneaker");
	}

	@Test
	void search_byCategoryId_filtersCorrectly() {
		var spec = ProductSpecifications.allOf(activeSpec(), ProductSpecifications.hasCategoryId(sneakers.getId()));

		Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 20));

		assertThat(page.getContent()).hasSize(2)
				.allSatisfy(p -> assertThat(p.getCategory().getId()).isEqualTo(sneakers.getId()));
	}

	@Test
	void search_byIsNewTrue_returnsOnlyNewProducts() {
		var spec = ProductSpecifications.allOf(activeSpec(), ProductSpecifications.isNew(true));

		Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(Product::getName)
				.containsExactlyInAnyOrder("Slick Formal Sneaker", "Elegant Wrap Dress");
	}

	@Test
	void search_byOnSaleTrue_returnsOnlyProductsWithCompareAtPrice() {
		var spec = ProductSpecifications.allOf(activeSpec(), ProductSpecifications.onSale(true));

		Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(Product::getName).containsExactly("Slick Formal Sneaker");
	}

	@Test
	void search_byPriceRange_filtersCorrectly() {
		var spec = ProductSpecifications.allOf(activeSpec(),
				ProductSpecifications.minPrice(new BigDecimal("3500.00")),
				ProductSpecifications.maxPrice(new BigDecimal("4000.00")));

		Page<Product> page = productRepository.findAll(spec, PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(Product::getName)
				.containsExactlyInAnyOrder("Trendy StepUp Pro", "Elegant Wrap Dress");
	}

	@Test
	void search_excludesInactiveProducts() {
		Page<Product> page = productRepository.findAll(activeSpec(), PageRequest.of(0, 20));

		assertThat(page.getContent()).extracting(Product::getName).doesNotContain("Discontinued Dress");
	}

	@Test
	void search_pagination_returnsCorrectPageAndTotal() {
		Page<Product> page = productRepository.findAll(activeSpec(), PageRequest.of(0, 2));

		assertThat(page.getContent()).hasSize(2);
		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	void findByIdInAndIsActiveTrue_excludesInactiveAndUnrequestedIds() {
		Product active = productRepository.findAll(activeSpec(), PageRequest.of(0, 1)).getContent().get(0);
		Product inactive = productRepository.findAll(PageRequest.of(0, 10)).getContent().stream()
				.filter(p -> !p.isActive()).findFirst().orElseThrow();

		var result = productRepository.findByIdInAndIsActiveTrue(java.util.List.of(active.getId(), inactive.getId()));

		assertThat(result).extracting(Product::getId).containsExactly(active.getId());
	}

}
