package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotNull;

public record InventoryReleaseRequest(@NotNull Long orderId) {
}
