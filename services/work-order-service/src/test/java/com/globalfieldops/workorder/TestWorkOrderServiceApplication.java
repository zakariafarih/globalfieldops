package com.globalfieldops.workorder;

import org.springframework.boot.SpringApplication;

public class TestWorkOrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(WorkOrderServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
