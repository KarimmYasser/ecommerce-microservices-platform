package com.ejada.ecommerce.inventory.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ejada.ecommerce.inventory.domain.InventoryReservation;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

	Optional<InventoryReservation> findByOrderId(Long orderId);

}
