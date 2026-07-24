package com.ejada.ecommerce.wallet.dto;

import jakarta.validation.constraints.NotBlank;

public record UserUpdateRequest(@NotBlank String fullName, String phone) {
}
