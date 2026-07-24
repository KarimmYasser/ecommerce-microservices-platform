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
import com.ejada.ecommerce.shop.dto.CartItemInput;
import com.ejada.ecommerce.shop.dto.CartResponse;
import com.ejada.ecommerce.shop.service.CartService;
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
@WebMvcTest(CartController.class)
class CartControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CartService cartService;

	@Test
	void getCart_unauthenticated_returns401() throws Exception {
		mockMvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "1")
	void getCart_authenticated_returns200() throws Exception {
		CartResponse cartResponse = new CartResponse(50L, List.of(), BigDecimal.ZERO);
		when(cartService.getCart(1L)).thenReturn(cartResponse);

		mockMvc.perform(get("/api/v1/cart"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(50));
	}

	@Test
	@WithMockUser(username = "1")
	void addItem_valid_returns200() throws Exception {
		CartResponse cartResponse = new CartResponse(50L, List.of(), BigDecimal.ZERO);
		when(cartService.addItem(eq(1L), any(CartItemInput.class))).thenReturn(cartResponse);

		mockMvc.perform(post("/api/v1/cart/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":10,\"variantId\":20,\"quantity\":2}"))
				.andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "1")
	void addItem_invalidQuantity_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/cart/items")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"productId\":10,\"variantId\":20,\"quantity\":-1}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(username = "1")
	void clearCart_returns204() throws Exception {
		mockMvc.perform(delete("/api/v1/cart"))
				.andExpect(status().isNoContent());
	}

}
