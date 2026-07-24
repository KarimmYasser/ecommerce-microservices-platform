package com.ejada.ecommerce.shop.dto;

import com.ejada.ecommerce.shop.domain.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
		Long id,
		String orderNumber,
		OrderStatus status,
		String failureReason,
		BigDecimal subtotal,
		BigDecimal discountTotal,
		BigDecimal grandTotal,
		String currency,
		String paymentTransactionId,
		List<OrderItemResponse> items,
		Instant createdAt) {
}
