package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record VariantInput(
		@NotBlank String sku,
		String size,
		String color,
		BigDecimal priceOverride,
		@PositiveOrZero int initialQuantity) {
}
