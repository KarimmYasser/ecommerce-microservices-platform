package com.ejada.ecommerce.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row per order that has called /inventory/reserve. Its existence (keyed
 * by the unique orderId) is what makes reserve/release idempotent: a repeat
 * reserve call for the same orderId is recognised and short-circuited instead
 * of double-reserving stock, and release looks the reservation up by orderId
 * rather than needing the caller to resend line items.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation extends BaseEntity {

	@Column(name = "order_id", nullable = false, unique = true)
	private Long orderId;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ReservationStatus status;

	@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private List<InventoryReservationItem> items = new ArrayList<>();

	public void addItem(InventoryReservationItem item) {
		items.add(item);
		item.setReservation(this);
	}

}
