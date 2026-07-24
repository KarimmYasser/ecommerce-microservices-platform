package com.ejada.ecommerce.inventory.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
		Long id,
		String name,
		String description,
		String brand,
		Long categoryId,
		BigDecimal basePrice,
		BigDecimal compareAtPrice,
		String currency,
		boolean isNew,
		BigDecimal ratingAverage,
		int ratingCount,
		List<ImageResponse> images,
		List<VariantResponse> variants) {
}
