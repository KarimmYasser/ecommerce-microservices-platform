package com.ejada.ecommerce.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
@Table(name = "cart_items")
public class CartItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cart_id", nullable = false)
	private Cart cart;

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "variant_id", nullable = false)
	private Long variantId;

	@Column(name = "quantity", nullable = false)
	private int quantity;

	/** Captured for display when added; checkout always re-fetches the authoritative price. */
	@Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPriceSnapshot;

}
