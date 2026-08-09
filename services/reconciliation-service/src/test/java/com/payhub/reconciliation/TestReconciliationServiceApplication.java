package com.payhub.reconciliation;

import org.springframework.boot.SpringApplication;

public class TestReconciliationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(ReconciliationServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
