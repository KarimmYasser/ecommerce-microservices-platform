package com.ejada.ecommerce.inventory.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.inventory.config.JwtService;
import com.ejada.ecommerce.inventory.dto.CategoryRequest;
import com.ejada.ecommerce.inventory.dto.CategoryResponse;
import com.ejada.ecommerce.inventory.dto.PageResponse;
import com.ejada.ecommerce.inventory.service.CategoryService;
import com.ejada.ecommerce.inventory.service.ProductService;
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
@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CategoryService categoryService;

	@MockitoBean
	private ProductService productService;

	@Test
	void list_isPublic_returns200() throws Exception {
		when(categoryService.findAll()).thenReturn(List.of(new CategoryResponse(1L, "Sneakers", "sneakers", null)));

		mockMvc.perform(get("/api/v1/categories")).andExpect(status().isOk());
	}

	@Test
	void productsInCategory_isPublic_returns200() throws Exception {
		when(productService.search(any(), any(Pageable.class))).thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

		mockMvc.perform(get("/api/v1/categories/1/products")).andExpect(status().isOk());
	}

	@Test
	void create_withoutToken_returns401() throws Exception {
		mockMvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(roles = "USER")
	void create_asNonAdmin_returns403() throws Exception {
		mockMvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isForbidden());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void create_asAdminWithValidBody_returns201() throws Exception {
		when(categoryService.create(any(CategoryRequest.class)))
				.thenReturn(new CategoryResponse(1L, "Sneakers", "sneakers", null));

		mockMvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Sneakers\",\"slug\":\"sneakers\"}"))
				.andExpect(status().isCreated());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void create_asAdminWithBlankName_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/categories").contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"\",\"slug\":\"sneakers\"}"))
				.andExpect(status().isBadRequest());
	}

}
