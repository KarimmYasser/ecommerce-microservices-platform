package com.ejada.ecommerce.inventory.mapper;

import static com.ejada.ecommerce.inventory.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.dto.CategoryResponse;

import org.junit.jupiter.api.Test;

class CategoryMapperTest {

	private final CategoryMapper mapper = new CategoryMapper();

	@Test
	void toResponse_mapsAllFields() {
		Category category = withId(Category.builder().name("Sneakers").slug("sneakers").parentId(5L).build(), 1L);

		CategoryResponse response = mapper.toResponse(category);

		assertThat(response.id()).isEqualTo(1L);
		assertThat(response.name()).isEqualTo("Sneakers");
		assertThat(response.slug()).isEqualTo("sneakers");
		assertThat(response.parentId()).isEqualTo(5L);
	}

	@Test
	void toResponse_whenParentIdNull_mapsNullThrough() {
		Category category = withId(Category.builder().name("Root").slug("root").build(), 1L);

		CategoryResponse response = mapper.toResponse(category);

		assertThat(response.parentId()).isNull();
	}

}
