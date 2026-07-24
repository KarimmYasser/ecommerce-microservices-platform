package com.ejada.ecommerce.inventory.mapper;

import com.ejada.ecommerce.inventory.domain.Product;
import com.ejada.ecommerce.inventory.domain.ProductImage;
import com.ejada.ecommerce.inventory.domain.ProductVariant;
import com.ejada.ecommerce.inventory.dto.ImageResponse;
import com.ejada.ecommerce.inventory.dto.ProductBatchItemResponse;
import com.ejada.ecommerce.inventory.dto.ProductDetailResponse;
import com.ejada.ecommerce.inventory.dto.ProductSummaryResponse;
import com.ejada.ecommerce.inventory.dto.VariantResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

	public ProductSummaryResponse toSummary(Product product) {
		return new ProductSummaryResponse(
				product.getId(),
				product.getName(),
				product.getBrand(),
				product.getCategory().getId(),
				product.getBasePrice(),
				product.getCompareAtPrice(),
				product.getCurrency(),
				product.isNew(),
				product.getRatingAverage(),
				product.getRatingCount(),
				primaryImageUrl(product));
	}

	public ProductDetailResponse toDetail(Product product) {
		List<ImageResponse> images = product.getImages().stream()
				.sorted(Comparator.comparingInt(ProductImage::getPosition))
				.map(img -> new ImageResponse(img.getId(), img.getUrl(), img.getPosition()))
				.toList();

		List<VariantResponse> variants = product.getVariants().stream()
				.map(v -> toVariantResponse(v, product))
				.toList();

		return new ProductDetailResponse(
				product.getId(),
				product.getName(),
				product.getDescription(),
				product.getBrand(),
				product.getCategory().getId(),
				product.getBasePrice(),
				product.getCompareAtPrice(),
				product.getCurrency(),
				product.isNew(),
				product.getRatingAverage(),
				product.getRatingCount(),
				images,
				variants);
	}

	public ProductBatchItemResponse toBatchItem(Product product) {
		return new ProductBatchItemResponse(
				product.getId(),
				product.getName(),
				product.getBasePrice(),
				product.getCurrency(),
				primaryImageUrl(product),
				product.isActive());
	}

	private VariantResponse toVariantResponse(ProductVariant variant, Product product) {
		int available = variant.getStockItem() != null ? variant.getStockItem().available() : 0;
		return new VariantResponse(
				variant.getId(),
				variant.getSku(),
				variant.getSize(),
				variant.getColor(),
				variant.effectivePrice(product.getBasePrice()),
				available);
	}

	private String primaryImageUrl(Product product) {
		return product.getImages().stream()
				.min(Comparator.comparingInt(ProductImage::getPosition))
				.map(ProductImage::getUrl)
				.orElse(null);
	}

}
