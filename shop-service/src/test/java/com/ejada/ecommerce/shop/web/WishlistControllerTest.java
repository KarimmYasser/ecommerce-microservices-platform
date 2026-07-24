package com.ejada.ecommerce.shop.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.shop.config.JwtService;
import com.ejada.ecommerce.shop.config.SecurityConfig;
import com.ejada.ecommerce.shop.dto.WishlistItemInput;
import com.ejada.ecommerce.shop.dto.WishlistItemResponse;
import com.ejada.ecommerce.shop.service.WishlistService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({ JwtService.class, SecurityConfig.class })
@WebMvcTest(WishlistController.class)
class WishlistControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private WishlistService wishlistService;

	@Test
	@WithMockUser(username = "1")
	void getWishlist_authenticated_returns200() throws Exception {
		WishlistItemResponse item = new WishlistItemResponse(10L, "Sneakers", new BigDecimal("50.00"), "USD", "img.jpg");
		when(wishlistService.getWishlist(1L)).thenReturn(List.of(item));

		mockMvc.perform(get("/api/v1/wishlist"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].name").value("Sneakers"));
	}

	@Test
	@WithMockUser(username = "1")
	void addItem_valid_returns201() throws Exception {
		WishlistItemResponse item = new WishlistItemResponse(10L, "Sneakers", new BigDecimal("50.00"), "USD", "img.jpg");
		when(wishlistService.addItem(eq(1L), any(WishlistItemInput.class))).thenReturn(item);

		mockMvc.perform(post("/api/v1/wishlist/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":10}"))
				.andExpect(status().isCreated());
	}

	@Test
	@WithMockUser(username = "1")
	void removeItem_returns204() throws Exception {
		mockMvc.perform(delete("/api/v1/wishlist/items/10"))
				.andExpect(status().isNoContent());
	}

}
