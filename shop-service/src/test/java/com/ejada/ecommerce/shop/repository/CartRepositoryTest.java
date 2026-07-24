package com.ejada.ecommerce.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.shop.domain.Cart;
import com.ejada.ecommerce.shop.domain.CartItem;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
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
class CartRepositoryTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("shop_repo_test").withUsername("test").withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Autowired
	private CartRepository cartRepository;

	@Autowired
	private CartItemRepository cartItemRepository;

	@Test
	void findByUserId_savesAndRetrievesCartWithItems() {
		Cart cart = Cart.builder().userId(1L).build();
		CartItem item = CartItem.builder()
				.cart(cart)
				.productId(10L)
				.variantId(20L)
				.quantity(2)
				.unitPriceSnapshot(new BigDecimal("50.00"))
				.build();
		cart.addItem(item);

		Cart saved = cartRepository.saveAndFlush(cart);

		Optional<Cart> found = cartRepository.findByUserId(1L);
		assertThat(found).isPresent();
		assertThat(found.get().getItems()).hasSize(1);
		assertThat(found.get().getItems().get(0).getProductId()).isEqualTo(10L);
	}

	@Test
	void findByCartIdAndProductIdAndVariantId_findsItem() {
		Cart cart = cartRepository.saveAndFlush(Cart.builder().userId(2L).build());
		CartItem item = CartItem.builder()
				.cart(cart)
				.productId(15L)
				.variantId(25L)
				.quantity(1)
				.unitPriceSnapshot(new BigDecimal("100.00"))
				.build();
		cartItemRepository.saveAndFlush(item);

		Optional<CartItem> foundItem = cartItemRepository.findByCartIdAndProductIdAndVariantId(cart.getId(), 15L, 25L);
		assertThat(foundItem).isPresent();
		assertThat(foundItem.get().getQuantity()).isEqualTo(1);
	}

}
