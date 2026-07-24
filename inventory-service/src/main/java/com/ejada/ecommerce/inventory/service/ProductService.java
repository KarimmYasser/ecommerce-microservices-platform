package com.ejada.ecommerce.inventory.service;

import com.ejada.ecommerce.inventory.dto.PageResponse;
import com.ejada.ecommerce.inventory.dto.ProductBatchItemResponse;
import com.ejada.ecommerce.inventory.dto.ProductCreateRequest;
import com.ejada.ecommerce.inventory.dto.ProductDetailResponse;
import com.ejada.ecommerce.inventory.dto.ProductFilter;
import com.ejada.ecommerce.inventory.dto.ProductSummaryResponse;
import com.ejada.ecommerce.inventory.dto.ProductUpdateRequest;
import com.ejada.ecommerce.inventory.dto.VariantInput;
import com.ejada.ecommerce.inventory.dto.VariantResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface ProductService {

	PageResponse<ProductSummaryResponse> search(ProductFilter filter, Pageable pageable);

	ProductDetailResponse getById(Long id);

	List<ProductBatchItemResponse> findBatch(List<Long> ids);

	ProductDetailResponse create(ProductCreateRequest request);

	ProductDetailResponse update(Long id, ProductUpdateRequest request);

	void deactivate(Long id);

	VariantResponse addVariant(Long productId, VariantInput input);

}
