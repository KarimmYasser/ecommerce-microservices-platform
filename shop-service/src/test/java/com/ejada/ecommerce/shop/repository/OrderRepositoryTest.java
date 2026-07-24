package com.ejada.ecommerce.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.shop.domain.Order;
import com.ejada.ecommerce.shop.domain.OrderItem;
import com.ejada.ecommerce.shop.domain.OrderStatus;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
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
class OrderRepositoryTest {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("shop_order_repo_test").withUsername("test").withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Autowired
	private OrderRepository orderRepository;

	@Test
	void findByIdAndUserId_persistsOrderAndItems() {
		Order order = Order.builder()
				.orderNumber("ORD-9999")
				.userId(1L)
				.status(OrderStatus.CONFIRMED)
				.subtotal(new BigDecimal("100.00"))
				.grandTotal(new BigDecimal("100.00"))
				.currency("USD")
				.build();

		OrderItem item = OrderItem.builder()
				.productId(10L)
				.variantId(20L)
				.productNameSnapshot("Test Product")
				.unitPrice(new BigDecimal("50.00"))
				.quantity(2)
				.lineTotal(new BigDecimal("100.00"))
				.build();
		order.addItem(item);

		Order saved = orderRepository.saveAndFlush(order);

		Optional<Order> found = orderRepository.findByIdAndUserId(saved.getId(), 1L);
		assertThat(found).isPresent();
		assertThat(found.get().getOrderNumber()).isEqualTo("ORD-9999");
		assertThat(found.get().getItems()).hasSize(1);
	}

	@Test
	void findByUserIdOrderByCreatedAtDesc_returnsPagedOrders() {
		orderRepository.saveAndFlush(Order.builder().orderNumber("ORD-1").userId(1L).status(OrderStatus.CONFIRMED).subtotal(BigDecimal.TEN).grandTotal(BigDecimal.TEN).currency("USD").build());
		orderRepository.saveAndFlush(Order.builder().orderNumber("ORD-2").userId(1L).status(OrderStatus.CONFIRMED).subtotal(BigDecimal.TEN).grandTotal(BigDecimal.TEN).currency("USD").build());

		Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10));
		assertThat(orders.getContent()).hasSize(2);
	}

}
