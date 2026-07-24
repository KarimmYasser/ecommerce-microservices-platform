package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckItem(
		@NotNull Long variantId,
		@Positive int quantity) {
}
