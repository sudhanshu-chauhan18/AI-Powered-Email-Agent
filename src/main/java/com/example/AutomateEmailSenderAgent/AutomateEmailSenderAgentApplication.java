package com.example.AutomateEmailSenderAgent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AutomateEmailSenderAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(AutomateEmailSenderAgentApplication.class, args);
		System.out.println("Application Starts");
	}

}
