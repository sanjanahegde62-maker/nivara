package com.example.rental_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
@SpringBootApplication
@EnableScheduling
public class RentalManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(RentalManagementApplication.class, args);
	}

}
