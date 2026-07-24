package com.ejada.ecommerce.inventory.dto;

import java.math.BigDecimal;

public record ProductBatchItemResponse(
		Long id,
		String name,
		BigDecimal basePrice,
		String currency,
		String primaryImageUrl,
		boolean active) {
}
