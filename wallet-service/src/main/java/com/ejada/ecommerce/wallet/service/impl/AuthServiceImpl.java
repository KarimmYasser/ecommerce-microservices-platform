package com.ejada.ecommerce.wallet.service.impl;

import com.ejada.ecommerce.wallet.config.JwtService;
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
import com.ejada.ecommerce.wallet.service.AuthService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AuthServiceImpl implements AuthService {

	private static final String DEFAULT_CURRENCY = "USD";

	private final UserRepository userRepository;
	private final WalletRepository walletRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	@Override
	@Transactional
	public RegisterResponse register(RegisterRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new DuplicateEmailException(request.email());
		}

		User user = User.builder()
				.email(request.email())
				.passwordHash(passwordEncoder.encode(request.password()))
				.fullName(request.fullName())
				.phone(request.phone())
				.build();
		userRepository.save(user);

		Wallet wallet = Wallet.builder()
				.user(user)
				.balance(BigDecimal.ZERO)
				.currency(DEFAULT_CURRENCY)
				.build();
		walletRepository.save(wallet);

		return new RegisterResponse(user.getId());
	}

	@Override
	@Transactional(readOnly = true)
	public LoginResponse login(LoginRequest request) {
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(InvalidCredentialsException::new);

		if (!user.isEnabled() || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new InvalidCredentialsException();
		}

		String token = jwtService.issue(user.getId(), user.getRole().name());
		return new LoginResponse(token, "Bearer", jwtService.expirationSeconds());
	}

}
