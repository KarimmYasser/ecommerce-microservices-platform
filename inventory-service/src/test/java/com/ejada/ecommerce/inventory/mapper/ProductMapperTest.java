package com.ejada.ecommerce.inventory.mapper;

import static com.ejada.ecommerce.inventory.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.domain.Product;
import com.ejada.ecommerce.inventory.domain.ProductImage;
import com.ejada.ecommerce.inventory.domain.ProductVariant;
import com.ejada.ecommerce.inventory.domain.StockItem;
import com.ejada.ecommerce.inventory.dto.ProductDetailResponse;
import com.ejada.ecommerce.inventory.dto.ProductSummaryResponse;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductMapperTest {

	private final ProductMapper mapper = new ProductMapper();

	private Product baseProduct() {
		Category category = withId(Category.builder().name("Sneakers").slug("sneakers").build(), 1L);
		return withId(Product.builder()
				.name("Slick Sneaker")
				.description("desc")
				.brand("StepUp")
				.category(category)
				.basePrice(new BigDecimal("2999.00"))
				.compareAtPrice(new BigDecimal("4999.00"))
				.currency("INR")
				.isNew(true)
				.build(), 10L);
	}

	@Test
	void toSummary_whenNoImages_primaryImageUrlIsNull() {
		ProductSummaryResponse response = mapper.toSummary(baseProduct());

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.categoryId()).isEqualTo(1L);
		assertThat(response.primaryImageUrl()).isNull();
	}

	@Test
	void toSummary_picksLowestPositionAsPrimaryImage() {
		Product product = baseProduct();
		product.addImage(ProductImage.builder().url("second.png").position(1).build());
		product.addImage(ProductImage.builder().url("first.png").position(0).build());

		ProductSummaryResponse response = mapper.toSummary(product);

		assertThat(response.primaryImageUrl()).isEqualTo("first.png");
	}

	@Test
	void toDetail_variantWithoutOverride_usesProductBasePriceAndZeroAvailabilityWhenNoStock() {
		Product product = baseProduct();
		product.addVariant(ProductVariant.builder().sku("SKU-1").size("42").build());

		ProductDetailResponse response = mapper.toDetail(product);

		assertThat(response.variants()).hasSize(1);
		assertThat(response.variants().get(0).price()).isEqualByComparingTo("2999.00");
		assertThat(response.variants().get(0).available()).isZero();
	}

	@Test
	void toDetail_variantWithPriceOverride_usesOverrideAndStockAvailability() {
		Product product = baseProduct();
		ProductVariant variant = ProductVariant.builder().sku("SKU-1").size("42").priceOverride(new BigDecimal("3500.00")).build();
		variant.attachStockItem(StockItem.builder().quantityOnHand(10).quantityReserved(3).build());
		product.addVariant(variant);

		ProductDetailResponse response = mapper.toDetail(product);

		assertThat(response.variants().get(0).price()).isEqualByComparingTo("3500.00");
		assertThat(response.variants().get(0).available()).isEqualTo(7);
	}

}
