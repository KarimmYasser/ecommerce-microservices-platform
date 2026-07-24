package com.ejada.ecommerce.wallet.service.impl;

import static com.ejada.ecommerce.wallet.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.wallet.config.JwtService;
import com.ejada.ecommerce.wallet.domain.Role;
import com.ejada.ecommerce.wallet.domain.User;
import com.ejada.ecommerce.wallet.domain.Wallet;
import com.ejada.ecommerce.wallet.dto.LoginRequest;
import com.ejada.ecommerce.wallet.dto.LoginResponse;
import com.ejada.ecommerce.wallet.dto.RegisterRequest;
import com.ejada.ecommerce.wallet.dto.RegisterResponse;
import com.ejada.ecommerce.wallet.exception.DuplicateEmailException;
import com.ejada.ecommerce.wallet.exception.InvalidCredentialsException;
import com.ejada.ecommerce.wallet.repository.UserRepository;
import com.ejada.ecommerce.wallet.repository.WalletRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private WalletRepository walletRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JwtService jwtService;

	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		authService = new AuthServiceImpl(userRepository, walletRepository, passwordEncoder, jwtService);
	}

	@Test
	void register_whenEmailUnique_createsUserAndZeroBalanceWallet() {
		when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
		when(passwordEncoder.encode("secret123")).thenReturn("hashed");
		when(userRepository.save(any(User.class))).thenAnswer(inv -> withId(inv.getArgument(0), 5L));

		RegisterResponse response = authService.register(new RegisterRequest("a@b.com", "secret123", "Ahmed", null));

		assertThat(response.userId()).isEqualTo(5L);
		verify(walletRepository).save(argThatWalletHasZeroBalance());
	}

	private Wallet argThatWalletHasZeroBalance() {
		return org.mockito.ArgumentMatchers.argThat(w -> w.getBalance().compareTo(BigDecimal.ZERO) == 0);
	}

	@Test
	void register_whenEmailTaken_throwsDuplicateEmailException() {
		when(userRepository.existsByEmail("a@b.com")).thenReturn(true);

		assertThatThrownBy(() -> authService.register(new RegisterRequest("a@b.com", "secret123", "Ahmed", null)))
				.isInstanceOf(DuplicateEmailException.class);
		verify(userRepository, never()).save(any());
	}

	@Test
	void login_whenCredentialsValid_returnsToken() {
		User user = withId(User.builder().email("a@b.com").passwordHash("hashed").fullName("Ahmed")
				.role(Role.USER).enabled(true).build(), 5L);
		when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
		when(jwtService.issue(5L, "USER")).thenReturn("token123");
		when(jwtService.expirationSeconds()).thenReturn(3600L);

		LoginResponse response = authService.login(new LoginRequest("a@b.com", "secret123"));

		assertThat(response.accessToken()).isEqualTo("token123");
		assertThat(response.tokenType()).isEqualTo("Bearer");
		assertThat(response.expiresIn()).isEqualTo(3600L);
	}

	@Test
	void login_whenEmailUnknown_throwsInvalidCredentialsException() {
		when(userRepository.findByEmail("nobody@b.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@b.com", "secret123")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void login_whenPasswordWrong_throwsInvalidCredentialsException() {
		User user = withId(User.builder().email("a@b.com").passwordHash("hashed").fullName("Ahmed")
				.role(Role.USER).enabled(true).build(), 5L);
		when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
		when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "wrong")))
				.isInstanceOf(InvalidCredentialsException.class);
		verify(jwtService, never()).issue(anyLong(), any());
	}

	@Test
	void login_whenUserDisabled_throwsInvalidCredentialsException() {
		User user = withId(User.builder().email("a@b.com").passwordHash("hashed").fullName("Ahmed")
				.role(Role.USER).enabled(false).build(), 5L);
		when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

		assertThatThrownBy(() -> authService.login(new LoginRequest("a@b.com", "secret123")))
				.isInstanceOf(InvalidCredentialsException.class);
	}

}
