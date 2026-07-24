package com.ejada.ecommerce.inventory.service.impl;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.dto.CategoryRequest;
import com.ejada.ecommerce.inventory.dto.CategoryResponse;
import com.ejada.ecommerce.inventory.exception.CategoryNotFoundException;
import com.ejada.ecommerce.inventory.exception.DuplicateSlugException;
import com.ejada.ecommerce.inventory.mapper.CategoryMapper;
import com.ejada.ecommerce.inventory.repository.CategoryRepository;
import com.ejada.ecommerce.inventory.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepository;
	private final CategoryMapper categoryMapper;

	@Override
	@Transactional(readOnly = true)
	public List<CategoryResponse> findAll() {
		return categoryRepository.findAll().stream()
				.map(categoryMapper::toResponse)
				.toList();
	}

	@Override
	@Transactional
	public CategoryResponse create(CategoryRequest request) {
		if (categoryRepository.existsBySlug(request.slug())) {
			throw new DuplicateSlugException(request.slug());
		}
		Category category = Category.builder()
				.name(request.name())
				.slug(request.slug())
				.parentId(request.parentId())
				.build();
		return categoryMapper.toResponse(categoryRepository.save(category));
	}

	@Override
	@Transactional
	public CategoryResponse update(Long id, CategoryRequest request) {
		Category category = categoryRepository.findById(id)
				.orElseThrow(() -> new CategoryNotFoundException(id));

		if (!category.getSlug().equals(request.slug()) && categoryRepository.existsBySlug(request.slug())) {
			throw new DuplicateSlugException(request.slug());
		}

		category.setName(request.name());
		category.setSlug(request.slug());
		category.setParentId(request.parentId());
		return categoryMapper.toResponse(category);
	}

}
