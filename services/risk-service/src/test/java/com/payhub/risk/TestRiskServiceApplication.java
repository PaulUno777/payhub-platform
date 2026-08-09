package com.payhub.risk;

import org.springframework.boot.SpringApplication;

public class TestRiskServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(RiskServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
