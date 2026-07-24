package com.ejada.ecommerce.inventory.repository;

import com.ejada.ecommerce.inventory.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

	List<Product> findByIdInAndIsActiveTrue(List<Long> ids);

}
