package com.ejada.ecommerce.inventory.controller;

import com.ejada.ecommerce.inventory.dto.CategoryRequest;
import com.ejada.ecommerce.inventory.dto.CategoryResponse;
import com.ejada.ecommerce.inventory.dto.PageResponse;
import com.ejada.ecommerce.inventory.dto.ProductFilter;
import com.ejada.ecommerce.inventory.dto.ProductSummaryResponse;
import com.ejada.ecommerce.inventory.service.CategoryService;
import com.ejada.ecommerce.inventory.service.ProductService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

	private final CategoryService categoryService;
	private final ProductService productService;

	@GetMapping
	public List<CategoryResponse> list() {
		return categoryService.findAll();
	}

	@GetMapping("/{id}/products")
	public PageResponse<ProductSummaryResponse> productsInCategory(@PathVariable Long id, @org.springdoc.core.annotations.ParameterObject Pageable pageable) {
		var filter = new ProductFilter(null, id, null, null, null, null);
		return productService.search(filter, pageable);
	}

	@PostMapping
	public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request));
	}

	@PutMapping("/{id}")
	public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
		return categoryService.update(id, request);
	}

}
