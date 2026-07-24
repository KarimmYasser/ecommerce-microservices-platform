package com.ejada.ecommerce.shop.service;

import com.ejada.ecommerce.shop.dto.OrderResponse;
import com.ejada.ecommerce.shop.dto.PageResponse;
import org.springframework.data.domain.Pageable;

public interface OrderService {

	OrderResponse checkout(Long userId, String couponCode);

	OrderResponse getOrderById(Long userId, Long orderId);

	PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable);

	OrderResponse cancelOrder(Long userId, Long orderId);

}
