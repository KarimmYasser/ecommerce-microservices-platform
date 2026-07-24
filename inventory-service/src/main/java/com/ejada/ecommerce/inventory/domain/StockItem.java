package com.ejada.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(name = "stock_items")
public class StockItem extends BaseEntity {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "variant_id", nullable = false, unique = true)
	private ProductVariant variant;

	@Column(name = "quantity_on_hand", nullable = false)
	@Builder.Default
	private int quantityOnHand = 0;

	@Column(name = "quantity_reserved", nullable = false)
	@Builder.Default
	private int quantityReserved = 0;

	/** Guards concurrent reserve/release against lost updates. */
	@Version
	@Column(name = "version", nullable = false)
	@Builder.Default
	private long version = 0L;

	public int available() {
		return quantityOnHand - quantityReserved;
	}

	public boolean canReserve(int quantity) {
		return available() >= quantity;
	}

	public void reserve(int quantity) {
		if (!canReserve(quantity)) {
			throw new IllegalStateException(
					"Cannot reserve %d units; only %d available".formatted(quantity, available()));
		}
		quantityReserved += quantity;
	}

	public void release(int quantity) {
		quantityReserved = Math.max(0, quantityReserved - quantity);
	}

}
