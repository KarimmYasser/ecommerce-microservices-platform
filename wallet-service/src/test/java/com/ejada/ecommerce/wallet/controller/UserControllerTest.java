package com.ejada.ecommerce.wallet.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.wallet.config.JwtService;
import com.ejada.ecommerce.wallet.domain.Role;
import com.ejada.ecommerce.wallet.dto.UserProfileResponse;
import com.ejada.ecommerce.wallet.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({ JwtService.class, com.ejada.ecommerce.wallet.config.SecurityConfig.class })
@WebMvcTest(UserController.class)
class UserControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private UserService userService;

	@Test
	void getMe_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
	}

	@Test
	@WithMockUser(username = "5")
	void getMe_withToken_returns200() throws Exception {
		when(userService.getProfile(5L)).thenReturn(new UserProfileResponse(5L, "a@b.com", "Ahmed", null, Role.USER));

		mockMvc.perform(get("/api/v1/users/me")).andExpect(status().isOk());
	}

	@Test
	@WithMockUser(username = "5")
	void updateMe_withValidBody_returns200() throws Exception {
		when(userService.updateProfile(eq(5L), org.mockito.ArgumentMatchers.any()))
				.thenReturn(new UserProfileResponse(5L, "a@b.com", "New Name", "999", Role.USER));

		mockMvc.perform(put("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new com.ejada.ecommerce.wallet.dto.UserUpdateRequest("New Name", "999"))))
				.andExpect(status().isOk());
	}

}
