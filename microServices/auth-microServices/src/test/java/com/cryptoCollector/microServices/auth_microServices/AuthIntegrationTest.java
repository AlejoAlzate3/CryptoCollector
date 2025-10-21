package com.cryptoCollector.microServices.auth_microServices;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
public class AuthIntegrationTest {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        registry.add("spring.config.import", () -> "");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("eureka.client.register-with-eureka", () -> "false");
        registry.add("eureka.client.fetch-registry", () -> "false");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    public static void startContainer() {
        postgres.start();
    }

    @AfterAll
    public static void stopContainer() {
        postgres.stop();
    }

    @Test
    public void testRegisterAndLogin() throws Exception {
        String base = "http://localhost:" + port;

        Map<String, String> register = Map.of(
                "firstName", "ITTest",
                "lastName", "User",
                "email", "ittest.user@example.com",
                "password", "Password123");

        ResponseEntity<String> regResp = restTemplate.postForEntity(base + "/api/auth/register", register,
                String.class);
        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM users WHERE email = ?",
                Integer.class,
                "ittest.user@example.com");
        assertThat(count).isEqualTo(1);

        Map<String, String> login = Map.of(
                "email", "ittest.user@example.com",
                "password", "Password123");

        ResponseEntity<Map<String, Object>> loginResp = restTemplate.postForEntity(base + "/api/auth/login", login,
                (Class) Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).containsKey("token");
    }

    @Test
    public void testDuplicateRegister() throws Exception {
        String base = "http://localhost:" + port;

        Map<String, String> register = Map.of(
                "firstName", "Dup",
                "lastName", "User",
                "email", "dup.user@example.com",
                "password", "Password123");

        ResponseEntity<String> regResp1 = restTemplate.postForEntity(base + "/api/auth/register", register,
                String.class);
        assertThat(regResp1.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<String> regResp2 = restTemplate.postForEntity(base + "/api/auth/register", register,
                String.class);
        assertThat(regResp2.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    public void testInvalidLogin() throws Exception {
        String base = "http://localhost:" + port;

        Map<String, String> login = Map.of(
                "email", "nonexistent.user@example.com",
                "password", "WrongPassword");

        ResponseEntity<String> loginResp = restTemplate.postForEntity(base + "/api/auth/login", login, String.class);
        assertThat(loginResp.getStatusCode().is4xxClientError()).isTrue();
    }

    @Test
    public void testJwtClaims() throws Exception {
        String base = "http://localhost:" + port;

        String email = "claim.user@example.com";
        Map<String, String> register = Map.of(
                "firstName", "Claim",
                "lastName", "User",
                "email", email,
                "password", "Password123");

        ResponseEntity<String> regResp = restTemplate.postForEntity(base + "/api/auth/register", register,
                String.class);
        assertThat(regResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        Map<String, String> login = Map.of(
                "email", email,
                "password", "Password123");

        ResponseEntity<Map<String, Object>> loginResp = restTemplate.postForEntity(base + "/api/auth/login", login,
                (Class) Map.class);
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).containsKey("token");

        assertThat(loginResp.getBody()).isNotNull();
        String token = (String) loginResp.getBody().get("token");
        String[] parts = token.split("\\.");
        assertThat(parts.length).isEqualTo(3);
        String payload = parts[1];

        int padding = (4 - (payload.length() % 4)) % 4;
        payload += "=".repeat(padding);
        byte[] decoded = java.util.Base64.getUrlDecoder().decode(payload);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(decoded);
        assertThat(node.has("sub")).isTrue();
        assertThat(node.get("sub").asText()).isEqualTo(email);
    }
}
