package com.ejada.ecommerce.wallet.mapper;

import com.ejada.ecommerce.wallet.domain.User;
import com.ejada.ecommerce.wallet.dto.UserProfileResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

	public UserProfileResponse toProfileResponse(User user) {
		return new UserProfileResponse(user.getId(), user.getEmail(), user.getFullName(), user.getPhone(), user.getRole());
	}

}
