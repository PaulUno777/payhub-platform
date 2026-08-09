package com.payhub.railadapter;

import org.springframework.boot.SpringApplication;

public class TestRailAdapterServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(RailAdapterServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
