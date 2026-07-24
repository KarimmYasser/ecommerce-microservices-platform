package com.ejada.ecommerce.shop.web;

import com.ejada.ecommerce.shop.dto.CheckoutRequest;
import com.ejada.ecommerce.shop.dto.OrderResponse;
import com.ejada.ecommerce.shop.dto.PageResponse;
import com.ejada.ecommerce.shop.service.OrderService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

	private final OrderService orderService;

	@PostMapping
	public ResponseEntity<OrderResponse> checkout(
			Principal principal,
			@RequestBody(required = false) CheckoutRequest request) {
		Long userId = getUserId(principal);
		String couponCode = request != null ? request.couponCode() : null;
		OrderResponse response = orderService.checkout(userId, couponCode);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping
	public ResponseEntity<PageResponse<OrderResponse>> getUserOrders(
			Principal principal,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size) {
		Long userId = getUserId(principal);
		Pageable pageable = PageRequest.of(page, size);
		return ResponseEntity.ok(orderService.getUserOrders(userId, pageable));
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderResponse> getOrderById(Principal principal, @PathVariable("id") Long id) {
		Long userId = getUserId(principal);
		return ResponseEntity.ok(orderService.getOrderById(userId, id));
	}

	@PostMapping("/{id}/cancel")
	public ResponseEntity<OrderResponse> cancelOrder(Principal principal, @PathVariable("id") Long id) {
		Long userId = getUserId(principal);
		return ResponseEntity.ok(orderService.cancelOrder(userId, id));
	}

	private Long getUserId(Principal principal) {
		if (principal == null || principal.getName() == null) {
			throw new IllegalStateException("Unauthenticated request");
		}
		return Long.parseLong(principal.getName());
	}

}
