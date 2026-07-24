package com.ejada.ecommerce.shop.dto;

import java.time.Instant;

public record ReviewResponse(
		Long id,
		Long productId,
		String authorNameSnapshot,
		int rating,
		String title,
		String body,
		Instant createdAt) {
}
