package com.ejada.ecommerce.wallet.service.impl;

import com.ejada.ecommerce.wallet.domain.User;
import com.ejada.ecommerce.wallet.dto.UserProfileResponse;
import com.ejada.ecommerce.wallet.dto.UserUpdateRequest;
import com.ejada.ecommerce.wallet.exception.UserNotFoundException;
import com.ejada.ecommerce.wallet.mapper.UserMapper;
import com.ejada.ecommerce.wallet.repository.UserRepository;
import com.ejada.ecommerce.wallet.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final UserMapper userMapper;

	@Override
	@Transactional(readOnly = true)
	public UserProfileResponse getProfile(Long userId) {
		return userMapper.toProfileResponse(findOrThrow(userId));
	}

	@Override
	@Transactional
	public UserProfileResponse updateProfile(Long userId, UserUpdateRequest request) {
		User user = findOrThrow(userId);
		user.setFullName(request.fullName());
		user.setPhone(request.phone());
		return userMapper.toProfileResponse(user);
	}

	private User findOrThrow(Long userId) {
		return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
	}

}
