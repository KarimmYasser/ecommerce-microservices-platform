package com.ejada.ecommerce.wallet.mapper;

import static com.ejada.ecommerce.wallet.support.EntityTestSupport.withId;
import static org.assertj.core.api.Assertions.assertThat;

import com.ejada.ecommerce.wallet.domain.TransactionStatus;
import com.ejada.ecommerce.wallet.domain.TransactionType;
import com.ejada.ecommerce.wallet.domain.WalletTransaction;
import com.ejada.ecommerce.wallet.dto.TransactionResponse;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WalletTransactionMapperTest {

	private final WalletTransactionMapper mapper = new WalletTransactionMapper();

	@Test
	void toResponse_mapsAllFields() {
		WalletTransaction transaction = withId(WalletTransaction.builder()
				.type(TransactionType.PAYMENT)
				.amount(new BigDecimal("99.99"))
				.balanceAfter(new BigDecimal("400.01"))
				.referenceId("order-1")
				.idempotencyKey("order-1")
				.status(TransactionStatus.SUCCESS)
				.build(), 10L);

		TransactionResponse response = mapper.toResponse(transaction);

		assertThat(response.id()).isEqualTo(10L);
		assertThat(response.type()).isEqualTo(TransactionType.PAYMENT);
		assertThat(response.amount()).isEqualByComparingTo("99.99");
		assertThat(response.balanceAfter()).isEqualByComparingTo("400.01");
		assertThat(response.referenceId()).isEqualTo("order-1");
		assertThat(response.status()).isEqualTo(TransactionStatus.SUCCESS);
	}

}
