package com.globalfieldops.audit;

import org.springframework.boot.SpringApplication;

public class TestAuditServiceApplication {

	public static void main(String[] args) {
		SpringApplication.from(AuditServiceApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
