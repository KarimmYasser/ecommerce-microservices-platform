package com.ejada.ecommerce.shop.service;

import static com.ejada.ecommerce.shop.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.shop.client.ResilientInventoryClient;
import com.ejada.ecommerce.shop.client.ResilientWalletClient;
import com.ejada.ecommerce.shop.client.dto.CreditRequest;
import com.ejada.ecommerce.shop.client.dto.DebitRequest;
import com.ejada.ecommerce.shop.client.dto.DebitResponse;
import com.ejada.ecommerce.shop.client.dto.InventoryReleaseRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReserveRequest;
import com.ejada.ecommerce.shop.client.dto.InventoryReserveResponse;
import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.domain.Cart;
import com.ejada.ecommerce.shop.domain.CartItem;
import com.ejada.ecommerce.shop.domain.Order;
import com.ejada.ecommerce.shop.domain.OrderItem;
import com.ejada.ecommerce.shop.domain.OrderStatus;
import com.ejada.ecommerce.shop.dto.OrderResponse;
import com.ejada.ecommerce.shop.exception.InsufficientStockException;
import com.ejada.ecommerce.shop.exception.PaymentFailedException;
import com.ejada.ecommerce.shop.exception.ResourceNotFoundException;
import com.ejada.ecommerce.shop.service.impl.OrderServiceImpl;
import com.ejada.ecommerce.shop.mapper.OrderMapper;
import com.ejada.ecommerce.shop.repository.CartRepository;
import com.ejada.ecommerce.shop.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private CartRepository cartRepository;

	@Mock
	private CartService cartService;

	@Mock
	private ResilientInventoryClient inventoryClient;

	@Mock
	private ResilientWalletClient walletClient;

	private OrderMapper orderMapper = new OrderMapper();

	private OrderServiceImpl orderService;

	private final Long userId = 1L;

	@BeforeEach
	void setUp() {
		orderService = new OrderServiceImpl(
				orderRepository,
				cartRepository,
				cartService,
				inventoryClient,
				walletClient,
				orderMapper);
	}

	@Test
	@DisplayName("Checkout happy path: reserves stock, debits wallet, confirms order, clears cart")
	void checkout_happyPath() {
		Cart cart = createSampleCart();
		when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

		ProductBatchItem productItem = new ProductBatchItem(10L, "Sample Product", new BigDecimal("50.00"), "USD", "img.jpg", true);
		when(inventoryClient.getProductsBatch(List.of(10L))).thenReturn(List.of(productItem));

		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
			Order o = inv.getArgument(0);
			return o.getId() == null ? withId(o, 100L) : o;
		});

		when(inventoryClient.reserve(any(InventoryReserveRequest.class)))
				.thenReturn(new InventoryReserveResponse(true, List.of()));

		when(walletClient.debit(eq(userId), any(DebitRequest.class)))
				.thenReturn(new DebitResponse(999L, new BigDecimal("150.00")));

		OrderResponse response = orderService.checkout(userId, null);

		assertThat(response).isNotNull();
		assertThat(response.status()).isEqualTo(OrderStatus.CONFIRMED);
		assertThat(response.grandTotal()).isEqualByComparingTo("100.00");
		assertThat(response.paymentTransactionId()).isEqualTo("999");

		verify(inventoryClient).reserve(any(InventoryReserveRequest.class));

		ArgumentCaptor<DebitRequest> debitCaptor = ArgumentCaptor.forClass(DebitRequest.class);
		verify(walletClient).debit(eq(userId), debitCaptor.capture());
		assertThat(debitCaptor.getValue().idempotencyKey()).isEqualTo("order-100");

		verify(cartService).clearCart(userId);
	}

	@Test
	@DisplayName("Checkout stock shortfall: fails reservation (409), marks order FAILED (OUT_OF_STOCK), does NOT debit wallet")
	void checkout_insufficientStock_compensation() {
		Cart cart = createSampleCart();
		when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

		ProductBatchItem productItem = new ProductBatchItem(10L, "Sample Product", new BigDecimal("50.00"), "USD", "img.jpg", true);
		when(inventoryClient.getProductsBatch(List.of(10L))).thenReturn(List.of(productItem));

		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
			Order o = inv.getArgument(0);
			return o.getId() == null ? withId(o, 100L) : o;
		});

		FeignException conflictEx = createFeignException(409, "Conflict - Stock Shortfall");
		when(inventoryClient.reserve(any(InventoryReserveRequest.class))).thenThrow(conflictEx);

		assertThatThrownBy(() -> orderService.checkout(userId, null))
				.isInstanceOf(InsufficientStockException.class);

		verify(walletClient, never()).debit(any(), any());
		verify(cartService, never()).clearCart(any());
	}

	@Test
	@DisplayName("Checkout insufficient funds: stock reserve ok, debit fails (402) -> releases stock, marks order FAILED (PAYMENT_FAILED)")
	void checkout_paymentFailed_compensation() {
		Cart cart = createSampleCart();
		when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

		ProductBatchItem productItem = new ProductBatchItem(10L, "Sample Product", new BigDecimal("50.00"), "USD", "img.jpg", true);
		when(inventoryClient.getProductsBatch(List.of(10L))).thenReturn(List.of(productItem));

		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
			Order o = inv.getArgument(0);
			return o.getId() == null ? withId(o, 100L) : o;
		});

		when(inventoryClient.reserve(any(InventoryReserveRequest.class)))
				.thenReturn(new InventoryReserveResponse(true, List.of()));

		FeignException paymentEx = createFeignException(402, "Payment Required - Insufficient Funds");
		when(walletClient.debit(eq(userId), any(DebitRequest.class))).thenThrow(paymentEx);

		assertThatThrownBy(() -> orderService.checkout(userId, null))
				.isInstanceOf(PaymentFailedException.class);

		ArgumentCaptor<InventoryReleaseRequest> releaseCaptor = ArgumentCaptor.forClass(InventoryReleaseRequest.class);
		verify(inventoryClient).release(releaseCaptor.capture());
		assertThat(releaseCaptor.getValue().orderId()).isEqualTo(100L);

		verify(cartService, never()).clearCart(any());
	}

	@Test
	@DisplayName("Cancel order: credits wallet and releases stock reservation")
	void cancelOrder_success() {
		Order order = withId(Order.builder()
				.userId(userId)
				.orderNumber("ORD-123")
				.status(OrderStatus.CONFIRMED)
				.subtotal(new BigDecimal("100.00"))
				.grandTotal(new BigDecimal("100.00"))
				.currency("USD")
				.build(), 100L);

		when(orderRepository.findByIdAndUserId(100L, userId)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

		OrderResponse response = orderService.cancelOrder(userId, 100L);

		assertThat(response.status()).isEqualTo(OrderStatus.CANCELLED);

		ArgumentCaptor<CreditRequest> creditCaptor = ArgumentCaptor.forClass(CreditRequest.class);
		verify(walletClient).credit(eq(userId), creditCaptor.capture());
		assertThat(creditCaptor.getValue().idempotencyKey()).isEqualTo("refund-order-100");

		verify(inventoryClient).release(any(InventoryReleaseRequest.class));
	}

	private Cart createSampleCart() {
		Cart cart = withId(Cart.builder().userId(userId).build(), 50L);
		CartItem item = withId(CartItem.builder()
				.cart(cart)
				.productId(10L)
				.variantId(20L)
				.quantity(2)
				.unitPriceSnapshot(new BigDecimal("50.00"))
				.build(), 501L);
		cart.addItem(item);
		return cart;
	}

	private FeignException createFeignException(int status, String message) {
		Request request = Request.create(Request.HttpMethod.POST, "/test", Map.of(), null, null, null);
		return FeignException.errorStatus("testMethod", feign.Response.builder()
				.status(status)
				.reason(message)
				.request(request)
				.build());
	}

}
