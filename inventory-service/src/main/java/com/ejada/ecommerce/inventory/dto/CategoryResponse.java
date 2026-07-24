package com.ejada.ecommerce.inventory.dto;

public record CategoryResponse(
		Long id,
		String name,
		String slug,
		Long parentId) {
}
