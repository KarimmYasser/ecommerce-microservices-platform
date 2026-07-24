package com.ejada.ecommerce.shop.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CartItemInput(
		@NotNull Long productId,
		@NotNull Long variantId,
		@Positive int quantity) {
}
