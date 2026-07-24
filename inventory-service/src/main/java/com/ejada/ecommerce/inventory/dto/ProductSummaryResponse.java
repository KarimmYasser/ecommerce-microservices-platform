package com.ejada.ecommerce.inventory.dto;

import java.math.BigDecimal;

public record ProductSummaryResponse(
		Long id,
		String name,
		String brand,
		Long categoryId,
		BigDecimal basePrice,
		BigDecimal compareAtPrice,
		String currency,
		boolean isNew,
		BigDecimal ratingAverage,
		int ratingCount,
		String primaryImageUrl) {
}
