package com.globalfieldops.technician;

import org.springframework.boot.SpringApplication;

public class TestTechnicianServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(TechnicianServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
