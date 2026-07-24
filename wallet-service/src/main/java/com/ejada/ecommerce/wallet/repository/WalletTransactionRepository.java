package com.ejada.ecommerce.wallet.repository;

import com.ejada.ecommerce.wallet.domain.WalletTransaction;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

	Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

	Page<WalletTransaction> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

}
