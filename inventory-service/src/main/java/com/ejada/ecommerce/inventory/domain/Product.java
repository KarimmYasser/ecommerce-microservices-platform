package com.ejada.ecommerce.inventory.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "products")
public class Product extends BaseEntity {

	@Column(name = "name", nullable = false)
	private String name;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "brand")
	private String brand;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@Column(name = "base_price", nullable = false, precision = 10, scale = 2)
	private BigDecimal basePrice;

	@Column(name = "compare_at_price", precision = 10, scale = 2)
	private BigDecimal compareAtPrice;

	@Column(name = "currency", nullable = false, length = 3)
	private String currency;

	@Column(name = "is_new", nullable = false)
	@Builder.Default
	private boolean isNew = false;

	@Column(name = "is_active", nullable = false)
	@Builder.Default
	private boolean isActive = true;

	@Column(name = "rating_average", precision = 2, scale = 1)
	@Builder.Default
	private BigDecimal ratingAverage = BigDecimal.ZERO;

	@Column(name = "rating_count", nullable = false)
	@Builder.Default
	private int ratingCount = 0;

	/**
	 * A product cannot meaningfully exist without at least being able to hold
	 * images/variants, and images/variants have no purpose detached from their
	 * product — cascading their lifecycle here is intentional, not a shortcut.
	 */
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("position ASC")
	@Builder.Default
	private List<ProductImage> images = new ArrayList<>();

	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@Builder.Default
	private List<ProductVariant> variants = new ArrayList<>();

	public void addImage(ProductImage image) {
		images.add(image);
		image.setProduct(this);
	}

	public void addVariant(ProductVariant variant) {
		variants.add(variant);
		variant.setProduct(this);
	}

}
