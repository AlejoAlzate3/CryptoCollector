package com.cryptoCollector.microServices.crypto_collector_micro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class CryptoCollectorMicroApplication {

	public static void main(String[] args) {
		SpringApplication.run(CryptoCollectorMicroApplication.class, args);
	}

}
