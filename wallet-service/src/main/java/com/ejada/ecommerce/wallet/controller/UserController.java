package com.ejada.ecommerce.wallet.controller;

import com.ejada.ecommerce.wallet.dto.UserProfileResponse;
import com.ejada.ecommerce.wallet.dto.UserUpdateRequest;
import com.ejada.ecommerce.wallet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	@GetMapping("/me")
	public UserProfileResponse getMe(Authentication authentication) {
		return userService.getProfile(currentUserId(authentication));
	}

	@PutMapping("/me")
	public UserProfileResponse updateMe(Authentication authentication, @Valid @RequestBody UserUpdateRequest request) {
		return userService.updateProfile(currentUserId(authentication), request);
	}

	private Long currentUserId(Authentication authentication) {
		return Long.valueOf(authentication.getName());
	}

}
