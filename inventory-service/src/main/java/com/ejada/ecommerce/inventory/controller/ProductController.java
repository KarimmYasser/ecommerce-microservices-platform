package com.ejada.ecommerce.inventory.controller;

import com.ejada.ecommerce.inventory.dto.PageResponse;
import com.ejada.ecommerce.inventory.dto.ProductBatchItemResponse;
import com.ejada.ecommerce.inventory.dto.ProductCreateRequest;
import com.ejada.ecommerce.inventory.dto.ProductDetailResponse;
import com.ejada.ecommerce.inventory.dto.ProductFilter;
import com.ejada.ecommerce.inventory.dto.ProductSummaryResponse;
import com.ejada.ecommerce.inventory.dto.ProductUpdateRequest;
import com.ejada.ecommerce.inventory.dto.VariantInput;
import com.ejada.ecommerce.inventory.dto.VariantResponse;
import com.ejada.ecommerce.inventory.service.ProductService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

	private final ProductService productService;

	@SecurityRequirement(name = "")
	@GetMapping
	public PageResponse<ProductSummaryResponse> search(
			@RequestParam(required = false) String q,
			@RequestParam(required = false) Long categoryId,
			@RequestParam(required = false) Boolean isNew,
			@RequestParam(required = false) Boolean onSale,
			@RequestParam(required = false) BigDecimal minPrice,
			@RequestParam(required = false) BigDecimal maxPrice,
			@org.springdoc.core.annotations.ParameterObject Pageable pageable) {
		var filter = new ProductFilter(q, categoryId, isNew, onSale, minPrice, maxPrice);
		return productService.search(filter, pageable);
	}

	/** Internal — enriches cart/order lines. Not routed publicly by the gateway. */
	@GetMapping("/batch")
	public List<ProductBatchItemResponse> batch(@RequestParam List<Long> ids) {
		return productService.findBatch(ids);
	}

	@SecurityRequirement(name = "")
	@GetMapping("/{id}")
	public ProductDetailResponse getById(@PathVariable Long id) {
		return productService.getById(id);
	}


	@PostMapping
	public ResponseEntity<ProductDetailResponse> create(@Valid @RequestBody ProductCreateRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request));
	}

	@PutMapping("/{id}")
	public ProductDetailResponse update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest request) {
		return productService.update(id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deactivate(@PathVariable Long id) {
		productService.deactivate(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/{id}/variants")
	public ResponseEntity<VariantResponse> addVariant(@PathVariable Long id, @Valid @RequestBody VariantInput input) {
		return ResponseEntity.status(HttpStatus.CREATED).body(productService.addVariant(id, input));
	}

}
