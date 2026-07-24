package com.ejada.ecommerce.shop.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

	@Column(name = "order_number", nullable = false, unique = true)
	private String orderNumber;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private OrderStatus status;

	@Column(name = "failure_reason")
	private String failureReason;

	@Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotal;

	@Column(name = "discount_total", nullable = false, precision = 12, scale = 2)
	@Builder.Default
	private BigDecimal discountTotal = BigDecimal.ZERO;

	@Column(name = "grand_total", nullable = false, precision = 12, scale = 2)
	private BigDecimal grandTotal;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "payment_transaction_id")
	private String paymentTransactionId;

	@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private List<OrderItem> items = new ArrayList<>();

	public void addItem(OrderItem item) {
		items.add(item);
		item.setOrder(this);
	}

}
