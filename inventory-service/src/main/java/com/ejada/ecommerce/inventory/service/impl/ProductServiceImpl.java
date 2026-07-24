package com.ejada.ecommerce.inventory.service.impl;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.domain.Product;
import com.ejada.ecommerce.inventory.domain.ProductImage;
import com.ejada.ecommerce.inventory.domain.ProductVariant;
import com.ejada.ecommerce.inventory.domain.StockItem;
import com.ejada.ecommerce.inventory.dto.ImageInput;
import com.ejada.ecommerce.inventory.dto.PageResponse;
import com.ejada.ecommerce.inventory.dto.ProductBatchItemResponse;
import com.ejada.ecommerce.inventory.dto.ProductCreateRequest;
import com.ejada.ecommerce.inventory.dto.ProductDetailResponse;
import com.ejada.ecommerce.inventory.dto.ProductFilter;
import com.ejada.ecommerce.inventory.dto.ProductSummaryResponse;
import com.ejada.ecommerce.inventory.dto.ProductUpdateRequest;
import com.ejada.ecommerce.inventory.dto.VariantInput;
import com.ejada.ecommerce.inventory.dto.VariantResponse;
import com.ejada.ecommerce.inventory.exception.CategoryNotFoundException;
import com.ejada.ecommerce.inventory.exception.DuplicateSkuException;
import com.ejada.ecommerce.inventory.exception.ProductNotFoundException;
import com.ejada.ecommerce.inventory.mapper.ProductMapper;
import com.ejada.ecommerce.inventory.repository.CategoryRepository;
import com.ejada.ecommerce.inventory.repository.ProductRepository;
import com.ejada.ecommerce.inventory.repository.ProductVariantRepository;
import com.ejada.ecommerce.inventory.repository.spec.ProductSpecifications;
import com.ejada.ecommerce.inventory.service.ProductService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class ProductServiceImpl implements ProductService {

	private final ProductRepository productRepository;
	private final CategoryRepository categoryRepository;
	private final ProductVariantRepository variantRepository;
	private final ProductMapper productMapper;

	@Override
	@Transactional(readOnly = true)
	public PageResponse<ProductSummaryResponse> search(ProductFilter filter, Pageable pageable) {
		var spec = ProductSpecifications.allOf(
				ProductSpecifications.isActive(),
				ProductSpecifications.nameOrDescriptionContains(filter.q()),
				ProductSpecifications.hasCategoryId(filter.categoryId()),
				ProductSpecifications.isNew(filter.isNew()),
				ProductSpecifications.onSale(filter.onSale()),
				ProductSpecifications.minPrice(filter.minPrice()),
				ProductSpecifications.maxPrice(filter.maxPrice()));

		Page<ProductSummaryResponse> page = productRepository.findAll(spec, pageable)
				.map(productMapper::toSummary);
		return PageResponse.of(page);
	}

	@Override
	@Transactional(readOnly = true)
	public ProductDetailResponse getById(Long id) {
		return productMapper.toDetail(findProductOrThrow(id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductBatchItemResponse> findBatch(List<Long> ids) {
		return productRepository.findByIdInAndIsActiveTrue(ids).stream()
				.map(productMapper::toBatchItem)
				.toList();
	}

	@Override
	@Transactional
	public ProductDetailResponse create(ProductCreateRequest request) {
		Category category = findCategoryOrThrow(request.categoryId());

		Product product = Product.builder()
				.name(request.name())
				.description(request.description())
				.brand(request.brand())
				.category(category)
				.basePrice(request.basePrice())
				.compareAtPrice(request.compareAtPrice())
				.currency(request.currency())
				.isNew(request.isNew())
				.build();

		if (request.images() != null) {
			for (ImageInput image : request.images()) {
				product.addImage(ProductImage.builder().url(image.url()).position(image.position()).build());
			}
		}
		if (request.variants() != null) {
			for (VariantInput variant : request.variants()) {
				product.addVariant(buildVariant(variant));
			}
		}

		return productMapper.toDetail(productRepository.save(product));
	}

	@Override
	@Transactional
	public ProductDetailResponse update(Long id, ProductUpdateRequest request) {
		Product product = findProductOrThrow(id);
		Category category = findCategoryOrThrow(request.categoryId());

		product.setName(request.name());
		product.setDescription(request.description());
		product.setBrand(request.brand());
		product.setCategory(category);
		product.setBasePrice(request.basePrice());
		product.setCompareAtPrice(request.compareAtPrice());
		product.setCurrency(request.currency());
		product.setNew(request.isNew());
		product.setActive(request.isActive());

		return productMapper.toDetail(product);
	}

	@Override
	@Transactional
	public void deactivate(Long id) {
		Product product = findProductOrThrow(id);
		product.setActive(false);
	}

	@Override
	@Transactional
	public VariantResponse addVariant(Long productId, VariantInput input) {
		Product product = findProductOrThrow(productId);
		ProductVariant variant = buildVariant(input);
		product.addVariant(variant);
		productRepository.save(product);

		ProductVariant saved = variant; // JPA identifier is populated post-flush via cascade on save above
		return new VariantResponse(
				saved.getId(),
				saved.getSku(),
				saved.getSize(),
				saved.getColor(),
				saved.effectivePrice(product.getBasePrice()),
				saved.getStockItem().available());
	}

	private ProductVariant buildVariant(VariantInput input) {
		if (variantRepository.existsBySku(input.sku())) {
			throw new DuplicateSkuException(input.sku());
		}
		ProductVariant variant = ProductVariant.builder()
				.sku(input.sku())
				.size(input.size())
				.color(input.color())
				.priceOverride(input.priceOverride())
				.build();
		StockItem stockItem = StockItem.builder()
				.quantityOnHand(input.initialQuantity())
				.quantityReserved(0)
				.build();
		variant.attachStockItem(stockItem);
		return variant;
	}

	private Product findProductOrThrow(Long id) {
		return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
	}

	private Category findCategoryOrThrow(Long id) {
		return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
	}

}
