package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotNull;

/** {@code delta} may be positive (restock) or negative (write-off/correction). */
public record StockAdjustRequest(@NotNull Integer delta) {
}
