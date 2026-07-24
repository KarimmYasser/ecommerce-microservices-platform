package com.ejada.ecommerce.shop.client.dto;

public record UnavailableItem(Long variantId, int requested, int available) {
}
