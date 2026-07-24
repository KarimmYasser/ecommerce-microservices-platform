package com.ejada.ecommerce.shop.dto;

import java.math.BigDecimal;

public record WishlistItemResponse(
		Long productId,
		String name,
		BigDecimal basePrice,
		String currency,
		String primaryImageUrl) {
}
