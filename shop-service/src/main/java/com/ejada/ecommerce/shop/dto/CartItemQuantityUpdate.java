package com.ejada.ecommerce.shop.dto;

import jakarta.validation.constraints.Positive;

public record CartItemQuantityUpdate(@Positive int quantity) {
}
