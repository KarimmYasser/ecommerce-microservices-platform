package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record InventoryReserveRequest(
		@NotNull Long orderId,
		@NotEmpty @Valid List<CheckItem> items) {
}
