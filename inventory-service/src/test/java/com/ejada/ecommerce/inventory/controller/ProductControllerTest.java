package com.ejada.ecommerce.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.inventory.config.JwtService;
import com.ejada.ecommerce.inventory.dto.PageResponse;
import com.ejada.ecommerce.inventory.dto.ProductDetailResponse;
import com.ejada.ecommerce.inventory.dto.ProductFilter;
import com.ejada.ecommerce.inventory.exception.ProductNotFoundException;
import com.ejada.ecommerce.inventory.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({ JwtService.class, com.ejada.ecommerce.inventory.config.SecurityConfig.class })
@WebMvcTest(ProductController.class)
class ProductControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	private ProductDetailResponse sampleDetail() {
		return new ProductDetailResponse(1L, "Sneaker", "desc", "StepUp", 1L,
				new BigDecimal("2999.00"), null, "INR", true, BigDecimal.ZERO, 0, List.of(), List.of());
	}

	@Test
	void search_isPublic_returns200() throws Exception {
		when(productService.search(any(ProductFilter.class), any(Pageable.class)))
				.thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/v1/products")).andExpect(status().isOk());
	}

	@Test
	void getById_whenFound_returns200WithBody() throws Exception {
		when(productService.getById(1L)).thenReturn(sampleDetail());

		mockMvc.perform(get("/api/v1/products/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Sneaker"));
	}

	@Test
	void getById_whenMissing_returns404() throws Exception {
		when(productService.getById(404L)).thenThrow(new ProductNotFoundException(404L));

		mockMvc.perform(get("/api/v1/products/404")).andExpect(status().isNotFound());
	}

	@Test
	void create_withoutToken_returns401() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "USER")
	void create_asNonAdmin_returns403() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void create_asAdminWithInvalidBody_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void create_asAdminWithValidBody_returns201() throws Exception {
		when(productService.create(any())).thenReturn(sampleDetail());
		String body = """
				{
				  "name": "Sneaker",
				  "categoryId": 1,
				  "basePrice": 2999.00,
				  "currency": "INR",
				  "isNew": true
				}
				""";

		mockMvc.perform(post("/api/v1/products").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
	}

	@Test
	void batch_isPublic_returns200() throws Exception {
		when(productService.findBatch(eq(List.of(1L, 2L)))).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/products/batch?ids=1,2")).andExpect(status().isOk());
	}

}
