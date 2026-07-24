package com.ejada.ecommerce.wallet.service;

import com.ejada.ecommerce.wallet.dto.UserProfileResponse;
import com.ejada.ecommerce.wallet.dto.UserUpdateRequest;

public interface UserService {

	UserProfileResponse getProfile(Long userId);

	UserProfileResponse updateProfile(Long userId, UserUpdateRequest request);

}
