package com.ejada.ecommerce.inventory.service.impl;

import static com.ejada.ecommerce.inventory.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.domain.Product;
import com.ejada.ecommerce.inventory.dto.ImageInput;
import com.ejada.ecommerce.inventory.dto.PageResponse;
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
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

	@Mock
	private ProductRepository productRepository;

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private ProductVariantRepository variantRepository;

	private ProductServiceImpl productService;

	@BeforeEach
	void setUp() {
		productService = new ProductServiceImpl(productRepository, categoryRepository, variantRepository, new ProductMapper());
	}

	private Category category(long id) {
		return withId(Category.builder().name("Sneakers").slug("sneakers").build(), id);
	}

	private ProductCreateRequest createRequest(Long categoryId) {
		return new ProductCreateRequest(
				"Slick Sneaker", "desc", "StepUp", categoryId,
				new BigDecimal("2999.00"), new BigDecimal("4999.00"), "INR", true,
				List.of(new ImageInput("https://img/1.png", 0)),
				List.of(new VariantInput("SKU-1", "42", "Black", null, 10)));
	}

	@Test
	void create_whenCategoryExists_savesProductWithImagesAndVariants() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L)));
		when(variantRepository.existsBySku("SKU-1")).thenReturn(false);
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> withId(inv.getArgument(0), 10L));

		ProductDetailResponse response = productService.create(createRequest(1L));

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.images()).hasSize(1);
		assertThat(response.variants()).hasSize(1);
		assertThat(response.variants().get(0).available()).isEqualTo(10);
	}

	@Test
	void create_whenCategoryMissing_throwsCategoryNotFoundException() {
		when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.create(createRequest(404L)))
				.isInstanceOf(CategoryNotFoundException.class);
	}

	@Test
	void create_whenVariantSkuDuplicate_throwsDuplicateSkuException() {
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L)));
		when(variantRepository.existsBySku("SKU-1")).thenReturn(true);

		assertThatThrownBy(() -> productService.create(createRequest(1L)))
				.isInstanceOf(DuplicateSkuException.class);
	}

	@Test
	void getById_whenMissing_throwsProductNotFoundException() {
		when(productRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.getById(404L)).isInstanceOf(ProductNotFoundException.class);
	}

	@Test
	void update_whenProductMissing_throwsProductNotFoundException() {
		when(productRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> productService.update(404L,
				new ProductUpdateRequest("N", "d", "B", 1L, BigDecimal.ONE, null, "INR", false, true)))
				.isInstanceOf(ProductNotFoundException.class);
	}

	@Test
	void deactivate_setsIsActiveFalse() {
		Product product = withId(Product.builder()
				.name("X").category(category(1L)).basePrice(BigDecimal.ONE).currency("INR").isActive(true).build(), 1L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));

		productService.deactivate(1L);

		assertThat(product.isActive()).isFalse();
	}

	@Test
	void addVariant_whenDuplicateSku_throwsDuplicateSkuException() {
		Product product = withId(Product.builder()
				.name("X").category(category(1L)).basePrice(BigDecimal.TEN).currency("INR").build(), 1L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(variantRepository.existsBySku(anyString())).thenReturn(true);

		assertThatThrownBy(() -> productService.addVariant(1L, new VariantInput("DUP", "M", "Red", null, 5)))
				.isInstanceOf(DuplicateSkuException.class);
	}

	@Test
	void addVariant_whenValid_returnsVariantResponseWithAvailability() {
		Product product = withId(Product.builder()
				.name("X").category(category(1L)).basePrice(BigDecimal.TEN).currency("INR").build(), 1L);
		when(productRepository.findById(1L)).thenReturn(Optional.of(product));
		when(variantRepository.existsBySku("NEW-SKU")).thenReturn(false);
		when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

		VariantResponse response = productService.addVariant(1L, new VariantInput("NEW-SKU", "M", "Red", null, 7));

		assertThat(response.sku()).isEqualTo("NEW-SKU");
		assertThat(response.available()).isEqualTo(7);
	}

	@Test
	void search_wrapsRepositoryPageIntoPageResponse() {
		Product product = withId(Product.builder()
				.name("X").category(category(1L)).basePrice(BigDecimal.TEN).currency("INR").build(), 1L);
		Pageable pageable = PageRequest.of(0, 20);
		when(productRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Product>>any(), org.mockito.ArgumentMatchers.eq(pageable)))
				.thenReturn(new PageImpl<>(List.of(product), pageable, 1));

		PageResponse<ProductSummaryResponse> response = productService.search(
				new ProductFilter(null, null, null, null, null, null), pageable);

		assertThat(response.content()).hasSize(1);
		assertThat(response.totalElements()).isEqualTo(1);
	}

}
