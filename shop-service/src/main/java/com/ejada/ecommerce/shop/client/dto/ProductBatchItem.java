package com.ejada.ecommerce.shop.client.dto;

import java.math.BigDecimal;

/** Mirrors inventory-service's ProductBatchItemResponse — see docs/api/inter-service-feign.md. */
public record ProductBatchItem(
		Long id,
		String name,
		BigDecimal basePrice,
		String currency,
		String primaryImageUrl,
		boolean active) {
}
