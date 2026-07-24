package com.ejada.ecommerce.wallet.service;

import com.ejada.ecommerce.wallet.dto.LoginRequest;
import com.ejada.ecommerce.wallet.dto.LoginResponse;
import com.ejada.ecommerce.wallet.dto.RegisterRequest;
import com.ejada.ecommerce.wallet.dto.RegisterResponse;

public interface AuthService {

	RegisterResponse register(RegisterRequest request);

	LoginResponse login(LoginRequest request);

}
