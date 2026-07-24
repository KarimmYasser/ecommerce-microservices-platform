package com.ejada.ecommerce.wallet.mapper;

import static com.ejada.ecommerce.wallet.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.wallet.domain.Role;
import com.ejada.ecommerce.wallet.domain.User;
import com.ejada.ecommerce.wallet.dto.UserProfileResponse;
import org.junit.jupiter.api.Test;

class UserMapperTest {

	private final UserMapper mapper = new UserMapper();

	@Test
	void toProfileResponse_mapsAllFields() {
		User user = withId(User.builder().email("a@b.com").passwordHash("hash").fullName("Ahmed")
				.phone("123").role(Role.ADMIN).enabled(true).build(), 5L);

		UserProfileResponse response = mapper.toProfileResponse(user);

		assertThat(response.id()).isEqualTo(5L);
		assertThat(response.email()).isEqualTo("a@b.com");
		assertThat(response.fullName()).isEqualTo("Ahmed");
		assertThat(response.phone()).isEqualTo("123");
		assertThat(response.role()).isEqualTo(Role.ADMIN);
	}

}
