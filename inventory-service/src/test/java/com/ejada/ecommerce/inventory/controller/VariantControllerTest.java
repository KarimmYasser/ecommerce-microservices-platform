package com.ejada.ecommerce.inventory.controller;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.inventory.config.JwtService;
import com.ejada.ecommerce.inventory.exception.InvalidStockAdjustmentException;
import com.ejada.ecommerce.inventory.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({ JwtService.class, com.ejada.ecommerce.inventory.config.SecurityConfig.class })
@WebMvcTest(VariantController.class)
class VariantControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private StockService stockService;

	@Test
	void adjustStock_withoutToken_returns401() throws Exception {
		mockMvc.perform(post("/api/v1/variants/1/stock/adjust").contentType(MediaType.APPLICATION_JSON)
				.content("{\"delta\":5}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "USER")
	void adjustStock_asNonAdmin_returns403() throws Exception {
		mockMvc.perform(post("/api/v1/variants/1/stock/adjust").contentType(MediaType.APPLICATION_JSON)
				.content("{\"delta\":5}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adjustStock_asAdmin_returns204() throws Exception {
		doNothing().when(stockService).adjustStock(1L, 5);

		mockMvc.perform(post("/api/v1/variants/1/stock/adjust").contentType(MediaType.APPLICATION_JSON)
				.content("{\"delta\":5}"))
				.andExpect(status().isNoContent());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void adjustStock_whenInvalid_returns400() throws Exception {
		doThrow(new InvalidStockAdjustmentException("would go negative"))
				.when(stockService).adjustStock(1L, -100);

		mockMvc.perform(post("/api/v1/variants/1/stock/adjust").contentType(MediaType.APPLICATION_JSON)
				.content("{\"delta\":-100}"))
				.andExpect(status().isBadRequest());
	}

}
