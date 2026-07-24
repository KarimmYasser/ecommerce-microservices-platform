package com.ejada.ecommerce.inventory.dto;

import java.util.List;

public record InventoryCheckResponse(
		boolean allAvailable,
		List<UnavailableItem> unavailable) {
}
