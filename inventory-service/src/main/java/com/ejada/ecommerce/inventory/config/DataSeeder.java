package com.ejada.ecommerce.inventory.config;

import com.ejada.ecommerce.inventory.domain.Category;
import com.ejada.ecommerce.inventory.domain.Product;
import com.ejada.ecommerce.inventory.domain.ProductImage;
import com.ejada.ecommerce.inventory.domain.ProductVariant;
import com.ejada.ecommerce.inventory.domain.StockItem;
import com.ejada.ecommerce.inventory.repository.CategoryRepository;
import com.ejada.ecommerce.inventory.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Populates a small, representative catalog (drawn from both source Figma
 * designs — see docs/figma-analysis.md) so the API has something to browse on
 * first run. Idempotent: skips entirely if any category already exists.
 * Disabled in tests via the "test" profile so test data stays deterministic.
 */
@Profile("!test")
@RequiredArgsConstructor
@Component
public class DataSeeder implements CommandLineRunner {

	private final CategoryRepository categoryRepository;
	private final ProductRepository productRepository;

	@Override
	public void run(String... args) {
		if (categoryRepository.count() > 0) {
			return;
		}

		Category formalWomen = categoryRepository.save(category("Formal Women", "formal-women"));
		Category formalMen = categoryRepository.save(category("Formal Men", "formal-men"));
		Category sneakers = categoryRepository.save(category("Sneakers", "sneakers"));
		categoryRepository.save(category("Casual Style", "casual-style"));

		productRepository.save(product(
				"Elegant Wrap Dress", "Flowing wrap dress in premium crepe.", "Modeva",
				formalWomen, new BigDecimal("3800.00"), null, "EGP", true,
				variant("MDV-WRP-S", "S", "Teal", 0, 12),
				variant("MDV-WRP-M", "M", "Teal", 0, 18)));

		productRepository.save(product(
				"Tailored Wool Blazer", "Classic-fit tailored blazer.", "Modeva",
				formalMen, new BigDecimal("4200.00"), new BigDecimal("5200.00"), "EGP", false,
				variant("MDV-BLZ-M", "M", "Charcoal", 0, 8),
				variant("MDV-BLZ-L", "L", "Charcoal", 0, 6)));

		productRepository.save(product(
				"Slick Formal Sneaker", "Leather sneaker with a formal silhouette.", "StepUp",
				sneakers, new BigDecimal("2999.00"), new BigDecimal("4999.00"), "INR", true,
				variant("SUP-FSK-41", "41", "Grey", 0, 15),
				variant("SUP-FSK-42", "42", "Grey", 0, 20)));

		productRepository.save(product(
				"Trendy StepUp Pro", "The flagship trainer.", "StepUp",
				sneakers, new BigDecimal("3999.00"), null, "INR", true,
				variant("SUP-PRO-42", "42", "White", 0, 25),
				variant("SUP-PRO-43", "43", "White", 0, 10)));
	}

	private Category category(String name, String slug) {
		return Category.builder().name(name).slug(slug).build();
	}

	private Product product(String name, String description, String brand, Category category,
			BigDecimal basePrice, BigDecimal compareAtPrice, String currency, boolean isNew,
			ProductVariant... variants) {
		Product product = Product.builder()
				.name(name)
				.description(description)
				.brand(brand)
				.category(category)
				.basePrice(basePrice)
				.compareAtPrice(compareAtPrice)
				.currency(currency)
				.isNew(isNew)
				.build();
		product.addImage(ProductImage.builder()
				.url("https://placehold.co/600x800?text=" + name.replace(" ", "+"))
				.position(0)
				.build());
		for (ProductVariant variant : variants) {
			product.addVariant(variant);
		}
		return product;
	}

	private ProductVariant variant(String sku, String size, String color, int reserved, int onHand) {
		ProductVariant variant = ProductVariant.builder().sku(sku).size(size).color(color).build();
		variant.attachStockItem(StockItem.builder().quantityOnHand(onHand).quantityReserved(reserved).build());
		return variant;
	}

}
