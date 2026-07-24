package com.ejada.ecommerce.wallet.dto;

import com.ejada.ecommerce.wallet.domain.Role;

public record UserProfileResponse(Long id, String email, String fullName, String phone, Role role) {
}
