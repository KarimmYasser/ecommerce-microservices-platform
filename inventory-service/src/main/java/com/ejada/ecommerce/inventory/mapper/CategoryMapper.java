package com.ejada.ecommerce.inventory.mapper;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.dto.CategoryResponse;

import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

	public CategoryResponse toResponse(Category category) {
		return new CategoryResponse(
				category.getId(),
				category.getName(),
				category.getSlug(),
				category.getParentId());
	}

}
