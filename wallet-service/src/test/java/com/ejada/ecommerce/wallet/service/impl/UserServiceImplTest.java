package com.ejada.ecommerce.wallet.service.impl;

import static com.ejada.ecommerce.wallet.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.wallet.domain.Role;
import com.ejada.ecommerce.wallet.domain.User;
import com.ejada.ecommerce.wallet.dto.UserProfileResponse;
import com.ejada.ecommerce.wallet.dto.UserUpdateRequest;
import com.ejada.ecommerce.wallet.exception.UserNotFoundException;
import com.ejada.ecommerce.wallet.mapper.UserMapper;
import com.ejada.ecommerce.wallet.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	private UserServiceImpl userService;

	@BeforeEach
	void setUp() {
		userService = new UserServiceImpl(userRepository, new UserMapper());
	}

	private User user() {
		return withId(User.builder().email("a@b.com").passwordHash("h").fullName("Ahmed")
				.phone("123").role(Role.USER).enabled(true).build(), 5L);
	}

	@Test
	void getProfile_whenFound_returnsMappedProfile() {
		when(userRepository.findById(5L)).thenReturn(Optional.of(user()));

		UserProfileResponse response = userService.getProfile(5L);

		assertThat(response.id()).isEqualTo(5L);
		assertThat(response.email()).isEqualTo("a@b.com");
	}

	@Test
	void getProfile_whenMissing_throwsUserNotFoundException() {
		when(userRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userService.getProfile(404L)).isInstanceOf(UserNotFoundException.class);
	}

	@Test
	void updateProfile_updatesNameAndPhone() {
		User user = user();
		when(userRepository.findById(5L)).thenReturn(Optional.of(user));

		UserProfileResponse response = userService.updateProfile(5L, new UserUpdateRequest("New Name", "999"));

		assertThat(response.fullName()).isEqualTo("New Name");
		assertThat(response.phone()).isEqualTo("999");
	}

}
