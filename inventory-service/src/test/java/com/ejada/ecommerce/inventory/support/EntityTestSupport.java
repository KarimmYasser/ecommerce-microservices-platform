package com.ejada.ecommerce.inventory.support;

import org.springframework.test.util.ReflectionTestUtils;

import com.ejada.ecommerce.inventory.domain.BaseEntity;

/**
 * BaseEntity.id has no public setter by design (ids are DB-generated, never
 * application-assigned). Unit tests that need an entity to look "as if
 * persisted" — without a database — use this instead of weakening that
 * invariant with a real setter.
 */
public final class EntityTestSupport {

	private EntityTestSupport() {
	}

	public static <T extends BaseEntity> T withId(T entity, Long id) {
		ReflectionTestUtils.setField(entity, "id", id);
		return entity;
	}

}
