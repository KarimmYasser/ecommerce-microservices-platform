package com.ejada.ecommerce.inventory.service;

import com.ejada.ecommerce.inventory.dto.CategoryRequest;
import com.ejada.ecommerce.inventory.dto.CategoryResponse;
import java.util.List;

public interface CategoryService {

	List<CategoryResponse> findAll();

	CategoryResponse create(CategoryRequest request);

	CategoryResponse update(Long id, CategoryRequest request);

}
