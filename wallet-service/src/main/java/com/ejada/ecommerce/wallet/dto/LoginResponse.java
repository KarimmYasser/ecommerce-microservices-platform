package com.ejada.ecommerce.wallet.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {
}
