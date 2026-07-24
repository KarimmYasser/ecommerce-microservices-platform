package com.ejada.ecommerce.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "slug", nullable = false, unique = true)
	private String slug;

	/**
	 * Optional self-reference for simple nesting. Kept as a plain id (not a JPA
	 * association) — this domain only ever needs a flat parent pointer, not
	 * traversable parent/child object graphs.
	 */
	@Column(name = "parent_id")
	private Long parentId;

}
