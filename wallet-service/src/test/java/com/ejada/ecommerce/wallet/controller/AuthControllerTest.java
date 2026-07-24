package com.ejada.ecommerce.wallet.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ejada.ecommerce.wallet.config.JwtService;
import com.ejada.ecommerce.wallet.dto.LoginRequest;
import com.ejada.ecommerce.wallet.dto.LoginResponse;
import com.ejada.ecommerce.wallet.dto.RegisterRequest;
import com.ejada.ecommerce.wallet.dto.RegisterResponse;
import com.ejada.ecommerce.wallet.exception.DuplicateEmailException;
import com.ejada.ecommerce.wallet.exception.InvalidCredentialsException;
import com.ejada.ecommerce.wallet.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({ JwtService.class, com.ejada.ecommerce.wallet.config.SecurityConfig.class })
@WebMvcTest(AuthController.class)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@Test
	void register_withValidBody_returns201() throws Exception {
		when(authService.register(any(RegisterRequest.class))).thenReturn(new RegisterResponse(1L));

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RegisterRequest("a@b.com", "secret123", "Ahmed", null))))
				.andExpect(status().isCreated());
	}

	@Test
	void register_withInvalidEmail_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RegisterRequest("not-an-email", "secret123", "Ahmed", null))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_withShortPassword_returns400() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RegisterRequest("a@b.com", "short", "Ahmed", null))))
				.andExpect(status().isBadRequest());
	}

	@Test
	void register_whenEmailTaken_returns409() throws Exception {
		when(authService.register(any(RegisterRequest.class))).thenThrow(new DuplicateEmailException("a@b.com"));

		mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new RegisterRequest("a@b.com", "secret123", "Ahmed", null))))
				.andExpect(status().isConflict());
	}

	@Test
	void login_withValidCredentials_returns200() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenReturn(new LoginResponse("token", "Bearer", 3600));

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new LoginRequest("a@b.com", "secret123"))))
				.andExpect(status().isOk());
	}

	@Test
	void login_withWrongCredentials_returns401() throws Exception {
		when(authService.login(any(LoginRequest.class))).thenThrow(new InvalidCredentialsException());

		mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(new LoginRequest("a@b.com", "wrong"))))
				.andExpect(status().isUnauthorized());
	}

}
