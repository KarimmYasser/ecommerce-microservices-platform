package com.ejada.ecommerce.shop.mapper;

import com.ejada.ecommerce.shop.domain.Order;
import com.ejada.ecommerce.shop.domain.OrderItem;
import com.ejada.ecommerce.shop.dto.OrderItemResponse;
import com.ejada.ecommerce.shop.dto.OrderResponse;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

	public OrderResponse toResponse(Order order) {
		return new OrderResponse(
				order.getId(),
				order.getOrderNumber(),
				order.getStatus(),
				order.getFailureReason(),
				order.getSubtotal(),
				order.getDiscountTotal(),
				order.getGrandTotal(),
				order.getCurrency(),
				order.getPaymentTransactionId(),
				order.getItems().stream().map(this::toItemResponse).toList(),
				order.getCreatedAt());
	}

	private OrderItemResponse toItemResponse(OrderItem item) {
		return new OrderItemResponse(
				item.getProductId(),
				item.getVariantId(),
				item.getProductNameSnapshot(),
				item.getUnitPrice(),
				item.getQuantity(),
				item.getLineTotal());
	}

}
