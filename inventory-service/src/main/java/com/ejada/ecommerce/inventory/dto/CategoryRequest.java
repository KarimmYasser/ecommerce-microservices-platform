package com.ejada.ecommerce.inventory.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
		@NotBlank String name,
		@NotBlank String slug,
		Long parentId) {
}
