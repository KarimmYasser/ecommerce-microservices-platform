package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record InventoryCheckRequest(@NotEmpty @Valid List<CheckItem> items) {
}
