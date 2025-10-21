package com.cryptoCollector.microServices.auth_microServices;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.config.import=",
		"spring.liquibase.enabled=false",
		"eureka.client.enabled=false",
		"spring.cloud.config.enabled=false",
		"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
		"spring.datasource.driverClassName=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthMicroServicesApplicationTests {

	@Test
	void contextLoads() {
	}

}
