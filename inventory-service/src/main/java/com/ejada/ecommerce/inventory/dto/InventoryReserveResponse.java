package com.ejada.ecommerce.inventory.dto;

import java.util.List;

public record InventoryReserveResponse(
		boolean reserved,
		List<UnavailableItem> shortfall) {

	public static InventoryReserveResponse success() {
		return new InventoryReserveResponse(true, List.of());
	}

	public static InventoryReserveResponse shortfall(List<UnavailableItem> shortfall) {
		return new InventoryReserveResponse(false, shortfall);
	}

}
