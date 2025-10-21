package com.cryptoCollector.microServices.auth_microServices;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthMicroServicesApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthMicroServicesApplication.class, args);
	}

}
