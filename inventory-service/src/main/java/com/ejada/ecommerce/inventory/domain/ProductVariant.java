package com.ejada.ecommerce.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "sku", nullable = false, unique = true)
	private String sku;

	@Column(name = "size")
	private String size;

	@Column(name = "color")
	private String color;

	@Column(name = "price_override", precision = 10, scale = 2)
	private BigDecimal priceOverride;

	/**
	 * Every purchasable SKU needs exactly one stock row and vice versa — cascading
	 * the stock item's lifecycle with its variant is intentional here, matching
	 * StockItem's own 1:1 "tracked by" relationship in the domain model.
	 */
	@OneToOne(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private StockItem stockItem;

	public void attachStockItem(StockItem stockItem) {
		this.stockItem = stockItem;
		stockItem.setVariant(this);
	}

	public BigDecimal effectivePrice(BigDecimal productBasePrice) {
		return priceOverride != null ? priceOverride : productBasePrice;
	}

}
