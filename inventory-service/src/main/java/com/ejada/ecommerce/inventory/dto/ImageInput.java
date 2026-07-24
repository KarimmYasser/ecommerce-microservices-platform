package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ImageInput(
		@NotBlank String url,
		@PositiveOrZero int position) {
}
