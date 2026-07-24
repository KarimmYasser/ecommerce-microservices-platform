package com.ejada.ecommerce.shop.dto;

import jakarta.validation.constraints.NotNull;

public record WishlistItemInput(@NotNull Long productId) {
}
