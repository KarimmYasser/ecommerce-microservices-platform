package com.ejada.ecommerce.shop.service.impl;

import com.ejada.ecommerce.shop.service.CartService;
import com.ejada.ecommerce.shop.service.OrderService;

import com.ejada.ecommerce.shop.client.ResilientInventoryClient;
import com.ejada.ecommerce.shop.client.ResilientWalletClient;
import com.ejada.ecommerce.shop.client.dto.CheckItem;
import com.ejada.ecommerce.shop.client.dto.CreditRequest;
import com.ejada.ecommerce.shop.client.dto.DebitRequest;
import com.ejada.ecommerce.shop.client.dto.DebitResponse;
import com.ejada.ecommerce.shop.client.dto.InventoryReleaseRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReserveRequest;
import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.domain.Cart;
import com.ejada.ecommerce.shop.domain.CartItem;
import com.ejada.ecommerce.shop.domain.Order;
import com.ejada.ecommerce.shop.domain.OrderItem;
import com.ejada.ecommerce.shop.domain.OrderStatus;
import com.ejada.ecommerce.shop.dto.OrderResponse;
import com.ejada.ecommerce.shop.dto.PageResponse;
import com.ejada.ecommerce.shop.exception.DownstreamServiceException;
import com.ejada.ecommerce.shop.exception.InsufficientStockException;
import com.ejada.ecommerce.shop.exception.PaymentFailedException;
import com.ejada.ecommerce.shop.exception.ResourceNotFoundException;
import com.ejada.ecommerce.shop.mapper.OrderMapper;
import com.ejada.ecommerce.shop.repository.CartRepository;
import com.ejada.ecommerce.shop.repository.OrderRepository;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {

	private final OrderRepository orderRepository;

	private final CartRepository cartRepository;

	private final CartService cartService;

	private final ResilientInventoryClient resilientInventoryClient;

	private final ResilientWalletClient resilientWalletClient;

	private final OrderMapper orderMapper;

	@Transactional
	@Override
	public OrderResponse checkout(Long userId, String couponCode) {
		Cart cart = cartRepository.findByUserId(userId)
				.orElseThrow(() -> new IllegalArgumentException("Cart is empty"));

		if (cart.getItems().isEmpty()) {
			throw new IllegalArgumentException("Cart is empty");
		}

		List<Long> productIds = cart.getItems().stream()
				.map(CartItem::getProductId)
				.distinct()
				.toList();

		Map<Long, ProductBatchItem> liveProducts;
		try {
			List<ProductBatchItem> batch = resilientInventoryClient.getProductsBatch(productIds);
			if (batch == null || batch.isEmpty()) {
				throw new DownstreamServiceException("Inventory service returned empty product details");
			}
			liveProducts = batch.stream().collect(Collectors.toMap(ProductBatchItem::id, Function.identity(), (a, b) -> a));
		} catch (Exception ex) {
			if (ex instanceof DownstreamServiceException dse) throw dse;
			throw new DownstreamServiceException("Inventory service unavailable for checkout", ex);
		}

		BigDecimal subtotal = BigDecimal.ZERO;
		String currency = "USD";

		Order order = Order.builder()
				.orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
				.userId(userId)
				.status(OrderStatus.PENDING)
				.discountTotal(BigDecimal.ZERO)
				.subtotal(BigDecimal.ZERO)
				.grandTotal(BigDecimal.ZERO)
				.currency(currency)
				.build();

		for (CartItem cartItem : cart.getItems()) {
			ProductBatchItem liveProduct = liveProducts.get(cartItem.getProductId());
			if (liveProduct == null) {
				throw new DownstreamServiceException("Missing product detail for product ID: " + cartItem.getProductId());
			}
			BigDecimal itemPrice = liveProduct.basePrice();
			currency = liveProduct.currency() != null ? liveProduct.currency() : currency;
			BigDecimal lineTotal = itemPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

			OrderItem orderItem = OrderItem.builder()
					.productId(cartItem.getProductId())
					.variantId(cartItem.getVariantId())
					.productNameSnapshot(liveProduct.name())
					.unitPrice(itemPrice)
					.quantity(cartItem.getQuantity())
					.lineTotal(lineTotal)
					.build();
			order.addItem(orderItem);
			subtotal = subtotal.add(lineTotal);
		}

		order.setSubtotal(subtotal);
		order.setGrandTotal(subtotal.subtract(order.getDiscountTotal()));
		order.setCurrency(currency);

		Order savedOrder = orderRepository.save(order);

		// Step 1: Reserve Stock
		List<CheckItem> reserveItems = cart.getItems().stream()
				.map(item -> new CheckItem(item.getVariantId(), item.getQuantity()))
				.toList();

		try {
			resilientInventoryClient.reserve(new InventoryReserveRequest(savedOrder.getId(), reserveItems));
		} catch (InsufficientStockException ex) {
			savedOrder.setStatus(OrderStatus.FAILED);
			savedOrder.setFailureReason("OUT_OF_STOCK");
			orderRepository.save(savedOrder);
			throw ex;
		} catch (FeignException ex) {
			if (ex.status() == 409) {
				savedOrder.setStatus(OrderStatus.FAILED);
				savedOrder.setFailureReason("OUT_OF_STOCK");
				orderRepository.save(savedOrder);
				throw new InsufficientStockException("Insufficient stock for checkout items");
			}
			savedOrder.setStatus(OrderStatus.FAILED);
			savedOrder.setFailureReason("INVENTORY_SERVICE_UNAVAILABLE");
			orderRepository.save(savedOrder);
			throw new DownstreamServiceException("Inventory service failed during reservation", ex);
		} catch (Exception ex) {
			savedOrder.setStatus(OrderStatus.FAILED);
			savedOrder.setFailureReason("INVENTORY_SERVICE_UNAVAILABLE");
			orderRepository.save(savedOrder);
			throw new DownstreamServiceException("Inventory service failed during reservation", ex);
		}

		// Step 2: Debit Wallet
		String idempotencyKey = "order-" + savedOrder.getId();
		DebitRequest debitRequest = new DebitRequest(savedOrder.getGrandTotal(), savedOrder.getCurrency(), idempotencyKey);

		try {
			DebitResponse debitResponse = resilientWalletClient.debit(userId, debitRequest);
			savedOrder.setPaymentTransactionId(String.valueOf(debitResponse.transactionId()));
		} catch (PaymentFailedException ex) {
			compensateReleaseStock(savedOrder.getId());
			savedOrder.setStatus(OrderStatus.FAILED);
			savedOrder.setFailureReason("PAYMENT_FAILED");
			orderRepository.save(savedOrder);
			throw ex;
		} catch (FeignException ex) {
			compensateReleaseStock(savedOrder.getId());
			if (ex.status() == 402) {
				savedOrder.setStatus(OrderStatus.FAILED);
				savedOrder.setFailureReason("PAYMENT_FAILED");
				orderRepository.save(savedOrder);
				throw new PaymentFailedException("Insufficient wallet funds for payment");
			}
			savedOrder.setStatus(OrderStatus.FAILED);
			savedOrder.setFailureReason("PAYMENT_SERVICE_UNAVAILABLE");
			orderRepository.save(savedOrder);
			throw new DownstreamServiceException("Wallet service failed during debit", ex);
		} catch (Exception ex) {
			compensateReleaseStock(savedOrder.getId());
			savedOrder.setStatus(OrderStatus.FAILED);
			savedOrder.setFailureReason("PAYMENT_SERVICE_UNAVAILABLE");
			orderRepository.save(savedOrder);
			throw new DownstreamServiceException("Wallet service failed during debit", ex);
		}

		// Step 3: Confirm & Clear Cart
		savedOrder.setStatus(OrderStatus.CONFIRMED);
		Order confirmedOrder = orderRepository.save(savedOrder);

		try {
			cartService.clearCart(userId);
		} catch (Exception ex) {
			log.warn("Failed to clear cart for user {} after successful order {}", userId, confirmedOrder.getId(), ex);
		}

		return orderMapper.toResponse(confirmedOrder);
	}

	@Transactional(readOnly = true)
	@Override
	public OrderResponse getOrderById(Long userId, Long orderId) {
		Order order = orderRepository.findByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
		return orderMapper.toResponse(order);
	}

	@Transactional(readOnly = true)
	@Override
	public PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
		Page<OrderResponse> page = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
				.map(orderMapper::toResponse);
		return PageResponse.of(page);
	}

	@Transactional
	@Override
	public OrderResponse cancelOrder(Long userId, Long orderId) {
		Order order = orderRepository.findByIdAndUserId(orderId, userId)
				.orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

		if (order.getStatus() != OrderStatus.CONFIRMED) {
			throw new IllegalStateException("Only CONFIRMED orders can be cancelled");
		}

		// Issue Refund
		String refundKey = "refund-order-" + order.getId();
		try {
			resilientWalletClient.credit(userId, new CreditRequest(order.getGrandTotal(), order.getCurrency(), refundKey));
		} catch (Exception ex) {
			log.error("Failed to credit refund for order {}", orderId, ex);
			throw new DownstreamServiceException("Wallet refund failed during cancellation", ex);
		}

		// Release Stock
		compensateReleaseStock(order.getId());

		order.setStatus(OrderStatus.CANCELLED);
		Order cancelledOrder = orderRepository.save(order);
		return orderMapper.toResponse(cancelledOrder);
	}

	private void compensateReleaseStock(Long orderId) {
		try {
			resilientInventoryClient.release(new InventoryReleaseRequest(orderId));
		} catch (Exception ex) {
			log.error("Stock release compensation failed for order {}", orderId, ex);
		}
	}

}
