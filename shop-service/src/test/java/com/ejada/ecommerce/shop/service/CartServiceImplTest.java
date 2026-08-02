package com.ejada.ecommerce.shop.service;

import static com.ejada.ecommerce.shop.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.shop.client.ResilientInventoryClient;
import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.domain.Cart;
import com.ejada.ecommerce.shop.domain.CartItem;
import com.ejada.ecommerce.shop.dto.CartItemInput;
import com.ejada.ecommerce.shop.dto.CartItemQuantityUpdate;
import com.ejada.ecommerce.shop.dto.CartResponse;
import com.ejada.ecommerce.shop.service.impl.CartServiceImpl;
import com.ejada.ecommerce.shop.exception.ResourceNotFoundException;
import com.ejada.ecommerce.shop.mapper.CartMapper;
import com.ejada.ecommerce.shop.repository.CartItemRepository;
import com.ejada.ecommerce.shop.repository.CartRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

	@Mock
	private CartRepository cartRepository;

	@Mock
	private CartItemRepository cartItemRepository;

	@Mock
	private ResilientInventoryClient inventoryClient;

	private CartMapper cartMapper = new CartMapper();

	private CartServiceImpl cartService;

	private final Long userId = 1L;

	@BeforeEach
	void setUp() {
		cartService = new CartServiceImpl(cartRepository, cartItemRepository, inventoryClient, cartMapper);
	}

	@Test
	@DisplayName("getCart: fetches live products from inventory and computes totals")
	void getCart_liveEnrichment() {
		Cart cart = createCart();
		when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

		ProductBatchItem productItem = new ProductBatchItem(10L, "Live Product", new BigDecimal("75.00"), "USD", "img.jpg", true);
		when(inventoryClient.getProductsBatch(List.of(10L))).thenReturn(List.of(productItem));

		CartResponse response = cartService.getCart(userId);

		assertThat(response).isNotNull();
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).name()).isEqualTo("Live Product");
		assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("75.00");
		assertThat(response.items().get(0).lineTotal()).isEqualByComparingTo("150.00");
		assertThat(response.items().get(0).priceUnconfirmed()).isFalse();
		assertThat(response.subtotal()).isEqualByComparingTo("150.00");
	}

	@Test
	@DisplayName("getCart fallback: inventory error -> uses unitPriceSnapshot and sets priceUnconfirmed=true")
	void getCart_inventoryFallback() {
		Cart cart = createCart();
		when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));

		when(inventoryClient.getProductsBatch(List.of(10L))).thenThrow(new RuntimeException("Inventory Down"));

		CartResponse response = cartService.getCart(userId);

		assertThat(response).isNotNull();
		assertThat(response.items()).hasSize(1);
		assertThat(response.items().get(0).unitPrice()).isEqualByComparingTo("50.00");
		assertThat(response.items().get(0).priceUnconfirmed()).isTrue();
		assertThat(response.subtotal()).isEqualByComparingTo("100.00");
	}

	@Test
	@DisplayName("addItem: adds new item when not present in cart")
	void addItem_newItem() {
		Cart cart = withId(Cart.builder().userId(userId).build(), 50L);

		when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByCartIdAndProductIdAndVariantId(50L, 10L, 20L)).thenReturn(Optional.empty());

		ProductBatchItem productItem = new ProductBatchItem(10L, "New Product", new BigDecimal("40.00"), "USD", "img.jpg", true);
		when(inventoryClient.getProductsBatch(List.of(10L))).thenReturn(List.of(productItem));

		when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

		CartItemInput input = new CartItemInput(10L, 20L, 2);
		CartResponse response = cartService.addItem(userId, input);

		assertThat(response).isNotNull();
		verify(cartItemRepository).save(any(CartItem.class));
	}

	@Test
	@DisplayName("updateItemQuantity: updates existing item quantity")
	void updateItemQuantity_success() {
		Cart cart = createCart();
		CartItem item = cart.getItems().get(0);

		when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByIdAndCartId(item.getId(), cart.getId())).thenReturn(Optional.of(item));

		CartResponse response = cartService.updateItemQuantity(userId, item.getId(), new CartItemQuantityUpdate(5));

		assertThat(item.getQuantity()).isEqualTo(5);
		verify(cartItemRepository).save(item);
	}

	@Test
	@DisplayName("removeItem: removes item from cart")
	void removeItem_success() {
		Cart cart = createCart();
		CartItem item = cart.getItems().get(0);

		when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
		when(cartItemRepository.findByIdAndCartId(item.getId(), cart.getId())).thenReturn(Optional.of(item));

		cartService.removeItem(userId, item.getId());

		verify(cartItemRepository).delete(item);
	}

	private Cart createCart() {
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

}
