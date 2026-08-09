package com.payhub.merchant;

import org.springframework.boot.SpringApplication;

public class TestMerchantServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(MerchantServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
