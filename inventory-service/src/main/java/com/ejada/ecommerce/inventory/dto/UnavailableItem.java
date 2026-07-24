package com.ejada.ecommerce.inventory.dto;

public record UnavailableItem(
		Long variantId,
		int requested,
		int available) {
}
