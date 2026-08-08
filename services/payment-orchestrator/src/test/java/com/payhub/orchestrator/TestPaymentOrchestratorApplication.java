package com.payhub.orchestrator;

import org.springframework.boot.SpringApplication;

public class TestPaymentOrchestratorApplication {

	public static void main(String[] args) {
		SpringApplication.from(PaymentOrchestratorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
