package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductUpdateRequest(
		@NotBlank String name,
		String description,
		String brand,
		@NotNull Long categoryId,
		@NotNull @Positive BigDecimal basePrice,
		BigDecimal compareAtPrice,
		@NotBlank @Size(min = 3, max = 3) String currency,
		boolean isNew,
		boolean isActive) {
}
