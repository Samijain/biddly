package com.biddly;

import org.springframework.boot.SpringApplication;

public class TestBiddlyBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(BiddlyBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
