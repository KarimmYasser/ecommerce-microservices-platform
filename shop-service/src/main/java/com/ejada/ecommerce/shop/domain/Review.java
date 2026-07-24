package com.ejada.ecommerce.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "reviews", uniqueConstraints = @UniqueConstraint(columnNames = { "product_id", "user_id" }))
public class Review extends BaseEntity {

	@Column(name = "product_id", nullable = false)
	private Long productId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "author_name_snapshot", nullable = false)
	private String authorNameSnapshot;

	@Column(name = "rating", nullable = false)
	private int rating;

	@Column(name = "title")
	private String title;

	@Column(name = "body", columnDefinition = "TEXT")
	private String body;

}
