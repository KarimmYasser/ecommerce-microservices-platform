package com.ejada.ecommerce.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.inventory.config.JwtService;
import com.ejada.ecommerce.inventory.dto.InventoryCheckResponse;
import com.ejada.ecommerce.inventory.dto.InventoryReleaseResponse;
import com.ejada.ecommerce.inventory.dto.InventoryReserveRequest;
import com.ejada.ecommerce.inventory.dto.InventoryReserveResponse;
import com.ejada.ecommerce.inventory.dto.UnavailableItem;
import com.ejada.ecommerce.inventory.service.StockService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Internal endpoints are unauthenticated at this layer (protected by network
 * topology instead — see SecurityConfig / docs/infrastructure/api-gateway.md),
 * so these tests hit them directly with no auth setup.
 */
@Import({ JwtService.class, com.ejada.ecommerce.inventory.config.SecurityConfig.class })
@WebMvcTest(InventoryInternalController.class)
class InventoryInternalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StockService stockService;

	@Test
	void check_withoutToken_returns200() throws Exception {
		when(stockService.check(any())).thenReturn(new InventoryCheckResponse(true, List.of()));

		mockMvc.perform(post("/inventory/check").contentType(MediaType.APPLICATION_JSON)
				.content("{\"items\":[{\"variantId\":1,\"quantity\":2}]}"))
				.andExpect(status().isOk());
	}

	@Test
	void reserve_whenSuccessful_returns200() throws Exception {
		when(stockService.reserve(any(InventoryReserveRequest.class))).thenReturn(InventoryReserveResponse.success());

		mockMvc.perform(post("/inventory/reserve").contentType(MediaType.APPLICATION_JSON)
				.content("{\"orderId\":100,\"items\":[{\"variantId\":1,\"quantity\":2}]}"))
				.andExpect(status().isOk());
	}

	@Test
	void reserve_whenShortfall_returns409() throws Exception {
		when(stockService.reserve(any(InventoryReserveRequest.class)))
				.thenReturn(InventoryReserveResponse.shortfall(List.of(new UnavailableItem(1L, 5, 2))));

		mockMvc.perform(post("/inventory/reserve").contentType(MediaType.APPLICATION_JSON)
				.content("{\"orderId\":100,\"items\":[{\"variantId\":1,\"quantity\":5}]}"))
				.andExpect(status().isConflict());
	}

	@Test
	void reserve_withInvalidBody_returns400() throws Exception {
		mockMvc.perform(post("/inventory/reserve").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void release_returns200() throws Exception {
		when(stockService.release(any())).thenReturn(new InventoryReleaseResponse(true));

		mockMvc.perform(post("/inventory/release").contentType(MediaType.APPLICATION_JSON)
				.content("{\"orderId\":100}"))
				.andExpect(status().isOk());
	}

}
