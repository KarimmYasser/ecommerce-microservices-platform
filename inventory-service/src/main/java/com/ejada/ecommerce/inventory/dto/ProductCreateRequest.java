package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record ProductCreateRequest(
		@NotBlank String name,
		String description,
		String brand,
		@NotNull Long categoryId,
		@NotNull @Positive BigDecimal basePrice,
		BigDecimal compareAtPrice,
		@NotBlank @Size(min = 3, max = 3) String currency,
		boolean isNew,
		@Valid List<ImageInput> images,
		@Valid List<VariantInput> variants) {
}
