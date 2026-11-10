package com.carlos.mindsync;

import org.springframework.boot.SpringApplication;

public class TestMindsyncApplication {

	public static void main(String[] args) {
		SpringApplication.from(MindsyncApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
