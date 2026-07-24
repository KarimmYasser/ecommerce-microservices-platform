package com.ejada.ecommerce.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
class WalletServiceApplicationTests {

	@Container
	static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
			.withDatabaseName("wallet_db_test")
			.withUsername("test")
			.withPassword("test");

	@DynamicPropertySource
	static void datasourceProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
		registry.add("spring.datasource.username", MYSQL::getUsername);
		registry.add("spring.datasource.password", MYSQL::getPassword);
	}

	@Test
	void contextLoads() {
	}

}
