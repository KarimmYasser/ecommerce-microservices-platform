package com.ejada.ecommerce.shop.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.shop.client.InventoryClient;
import com.ejada.ecommerce.shop.client.dto.ProductBatchItem;
import com.ejada.ecommerce.shop.domain.WishlistItem;
import com.ejada.ecommerce.shop.dto.WishlistItemInput;
import com.ejada.ecommerce.shop.dto.WishlistItemResponse;
import com.ejada.ecommerce.shop.service.impl.WishlistServiceImpl;
import com.ejada.ecommerce.shop.mapper.WishlistMapper;
import com.ejada.ecommerce.shop.repository.WishlistItemRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WishlistServiceImplTest {

	@Mock
	private WishlistItemRepository wishlistItemRepository;

	@Mock
	private InventoryClient inventoryClient;

	private WishlistMapper wishlistMapper = new WishlistMapper();

	private WishlistServiceImpl wishlistService;

	private final Long userId = 1L;

	@BeforeEach
	void setUp() {
		wishlistService = new WishlistServiceImpl(wishlistItemRepository, inventoryClient, wishlistMapper);
	}

	@Test
	void getWishlist_success() {
		WishlistItem item = WishlistItem.builder().userId(userId).productId(10L).build();
		when(wishlistItemRepository.findByUserId(userId)).thenReturn(List.of(item));

		ProductBatchItem productItem = new ProductBatchItem(10L, "Saved Product", new BigDecimal("99.99"), "USD", "img.jpg", true);
		when(inventoryClient.getProductsBatch(List.of(10L))).thenReturn(List.of(productItem));

		List<WishlistItemResponse> wishlist = wishlistService.getWishlist(userId);

		assertThat(wishlist).hasSize(1);
		assertThat(wishlist.get(0).name()).isEqualTo("Saved Product");
		assertThat(wishlist.get(0).basePrice()).isEqualByComparingTo("99.99");
	}

	@Test
	void addItem_success() {
		when(wishlistItemRepository.findByUserIdAndProductId(userId, 10L)).thenReturn(Optional.empty());
		when(wishlistItemRepository.save(any(WishlistItem.class))).thenAnswer(inv -> inv.getArgument(0));

		WishlistItemResponse response = wishlistService.addItem(userId, new WishlistItemInput(10L));

		assertThat(response.productId()).isEqualTo(10L);
		verify(wishlistItemRepository).save(any(WishlistItem.class));
	}

	@Test
	void removeItem_success() {
		WishlistItem item = WishlistItem.builder().userId(userId).productId(10L).build();
		when(wishlistItemRepository.findByUserIdAndProductId(userId, 10L)).thenReturn(Optional.of(item));

		wishlistService.removeItem(userId, 10L);

		verify(wishlistItemRepository).delete(item);
	}

}
