package com.cryptoCollector.microServices.auth_microServices.controller;

import com.cryptoCollector.microServices.auth_microServices.dto.LoginRequest;
import com.cryptoCollector.microServices.auth_microServices.dto.RegisterRequest;
import com.cryptoCollector.microServices.auth_microServices.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController - Tests de Integración")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register - Debe registrar un nuevo usuario exitosamente")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@test.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.firstName").value("Juan"))
                .andExpect(jsonPath("$.lastName").value("Pérez"))
                .andExpect(jsonPath("$.email").value("juan.perez@test.com"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @DisplayName("POST /api/auth/register - Debe fallar con email duplicado (409 Conflict)")
    void shouldFailToRegisterDuplicateEmail() throws Exception {
        RegisterRequest firstRequest = RegisterRequest.builder()
                .firstName("María")
                .lastName("González")
                .email("maria.gonzalez@test.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)));

        RegisterRequest duplicateRequest = RegisterRequest.builder()
                .firstName("María")
                .lastName("Otra")
                .email("maria.gonzalez@test.com")
                .password("DifferentPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("ya está registrado")))
                .andExpect(jsonPath("$.message").value(containsString("maria.gonzalez@test.com")));
    }

    @Test
    @DisplayName("POST /api/auth/register - Debe fallar con datos inválidos (400 Bad Request)")
    void shouldFailToRegisterWithInvalidData() throws Exception {
        RegisterRequest invalidRequest = RegisterRequest.builder()
                .firstName("")
                .lastName("")
                .email("invalid-email")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/login - Debe hacer login exitosamente con credenciales correctas")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Carlos")
                .lastName("Rodríguez")
                .email("carlos.rodriguez@test.com")
                .password("SecurePassword123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("carlos.rodriguez@test.com")
                .password("SecurePassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").value(not(emptyString())));
    }

    @Test
    @DisplayName("POST /api/auth/login - Debe fallar con email no registrado (401 Unauthorized)")
    void shouldFailToLoginWithNonExistentEmail() throws Exception {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("noexiste@test.com")
                .password("Password123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Debe fallar con contraseña incorrecta (401 Unauthorized)")
    void shouldFailToLoginWithWrongPassword() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Ana")
                .lastName("Martínez")
                .email("ana.martinez@test.com")
                .password("CorrectPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)));

        LoginRequest loginRequest = LoginRequest.builder()
                .email("ana.martinez@test.com")
                .password("WrongPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Debe fallar con datos inválidos (400 Bad Request)")
    void shouldFailToLoginWithInvalidData() throws Exception {
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("not-an-email")
                .password("")
                .build();

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Flujo completo: Register -> Login -> Token válido")
    void shouldCompleteFullAuthenticationFlow() throws Exception {
        RegisterRequest registerRequest = RegisterRequest.builder()
                .firstName("Pedro")
                .lastName("López")
                .email("pedro.lopez@test.com")
                .password("MySecurePassword123!")
                .build();

        String registerResponse = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        System.out.println("Register response: " + registerResponse);

        LoginRequest loginRequest = LoginRequest.builder()
                .email("pedro.lopez@test.com")
                .password("MySecurePassword123!")
                .build();

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        System.out.println("Login response: " + loginResponse);

        var authResponse = objectMapper.readTree(loginResponse);
        String token = authResponse.get("token").asText();

        assertThat(token, not(emptyString()));
        assertThat(token.length(), greaterThan(20));
    }

    @Test
    @DisplayName("Debe validar que las contraseñas se encripten correctamente")
    void shouldEncryptPasswordsCorrectly() throws Exception {
        String plainPassword = "PlainTextPassword123!";
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Test")
                .lastName("Security")
                .email("security.test@test.com")
                .password(plainPassword)
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then - Verificar que la contraseña en DB está encriptada
        var user = userRepository.findByEmail("security.test@test.com");
        assertTrue(user.isPresent());

        // La contraseña encriptada NO debe ser igual a la original
        assertNotEquals(plainPassword, user.get().getPassword());

        // La contraseña encriptada debe empezar con $2a$ (BCrypt)
        assertTrue(user.get().getPassword().startsWith("$2a$") ||
                user.get().getPassword().startsWith("$2b$"));
    }
}
