package com.ejada.ecommerce.inventory.service.impl;

import static com.ejada.ecommerce.inventory.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.dto.CategoryRequest;
import com.ejada.ecommerce.inventory.dto.CategoryResponse;
import com.ejada.ecommerce.inventory.exception.CategoryNotFoundException;
import com.ejada.ecommerce.inventory.exception.DuplicateSlugException;
import com.ejada.ecommerce.inventory.mapper.CategoryMapper;
import com.ejada.ecommerce.inventory.repository.CategoryRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

	@Mock
	private CategoryRepository categoryRepository;

	private CategoryServiceImpl categoryService;

	@BeforeEach
	void setUp() {
		categoryService = new CategoryServiceImpl(categoryRepository, new CategoryMapper());
	}

	@Test
	void create_whenSlugUnique_savesAndReturnsResponse() {
		when(categoryRepository.existsBySlug("sneakers")).thenReturn(false);
		when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> withId(inv.getArgument(0), 1L));

		CategoryResponse response = categoryService.create(new CategoryRequest("Sneakers", "sneakers", null));

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("Sneakers");
		assertThat(response.slug()).isEqualTo("sneakers");
	}

	@Test
	void create_whenSlugAlreadyExists_throwsDuplicateSlugException() {
		when(categoryRepository.existsBySlug("sneakers")).thenReturn(true);

		assertThatThrownBy(() -> categoryService.create(new CategoryRequest("Sneakers", "sneakers", null)))
				.isInstanceOf(DuplicateSlugException.class);
	}

	@Test
	void update_whenCategoryMissing_throwsCategoryNotFoundException() {
		when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> categoryService.update(404L, new CategoryRequest("X", "x", null)))
				.isInstanceOf(CategoryNotFoundException.class);
	}

	@Test
	void update_whenSlugUnchanged_doesNotCheckUniqueness() {
		Category existing = withId(Category.builder().name("Old").slug("old-slug").build(), 1L);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));

		CategoryResponse response = categoryService.update(1L, new CategoryRequest("New Name", "old-slug", null));

		assertThat(response.name()).isEqualTo("New Name");
		assertThat(response.slug()).isEqualTo("old-slug");
	}

	@Test
	void update_whenNewSlugCollidesWithAnotherCategory_throwsDuplicateSlugException() {
		Category existing = withId(Category.builder().name("Old").slug("old-slug").build(), 1L);
		when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
		when(categoryRepository.existsBySlug("taken-slug")).thenReturn(true);

		assertThatThrownBy(() -> categoryService.update(1L, new CategoryRequest("Old", "taken-slug", null)))
				.isInstanceOf(DuplicateSlugException.class);
	}

}
