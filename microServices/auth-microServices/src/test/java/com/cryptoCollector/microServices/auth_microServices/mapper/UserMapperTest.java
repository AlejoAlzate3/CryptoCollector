package com.cryptoCollector.microServices.auth_microServices.mapper;

import com.cryptoCollector.microServices.auth_microServices.dto.RegisterRequest;
import com.cryptoCollector.microServices.auth_microServices.dto.UserResponse;
import com.cryptoCollector.microServices.auth_microServices.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UserMapper - Tests Unitarios")
class UserMapperTest {

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapper();
    }

    @Test
    @DisplayName("Debe convertir RegisterRequest a User correctamente")
    void shouldMapRegisterRequestToUser() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("María")
                .lastName("García")
                .email("maria.garcia@test.com")
                .password("SecurePassword123!")
                .build();

        User user = userMapper.toEntity(request);

        assertNotNull(user);
        assertEquals("María", user.getFirstName());
        assertEquals("García", user.getLastName());
        assertEquals("maria.garcia@test.com", user.getEmail());
        assertNull(user.getId());
        assertNull(user.getPassword());
    }

    @Test
    @DisplayName("Debe convertir User a UserResponse correctamente")
    void shouldMapUserToUserResponse() {
        User user = User.builder()
                .id(10L)
                .firstName("Carlos")
                .lastName("Rodríguez")
                .email("carlos.rodriguez@test.com")
                .password("$2a$10$encodedPassword")
                .build();

        UserResponse response = userMapper.toResponse(user);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Carlos", response.getFirstName());
        assertEquals("Rodríguez", response.getLastName());
        assertEquals("carlos.rodriguez@test.com", response.getEmail());
    }

    @Test
    @DisplayName("Debe manejar RegisterRequest con campos vacíos")
    void shouldHandleEmptyRegisterRequest() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("")
                .lastName("")
                .email("")
                .password("")
                .build();

        User user = userMapper.toEntity(request);

        assertNotNull(user);
        assertEquals("", user.getFirstName());
        assertEquals("", user.getLastName());
        assertEquals("", user.getEmail());
    }

    @Test
    @DisplayName("Debe manejar User con ID null")
    void shouldHandleUserWithNullId() {
        User user = User.builder()
                .id(null)
                .firstName("Pedro")
                .lastName("López")
                .email("pedro.lopez@test.com")
                .password("hashedPassword")
                .build();

        UserResponse response = userMapper.toResponse(user);

        assertNotNull(response);
        assertNull(response.getId());
        assertEquals("Pedro", response.getFirstName());
        assertEquals("López", response.getLastName());
        assertEquals("pedro.lopez@test.com", response.getEmail());
    }

    @Test
    @DisplayName("Debe preservar todos los campos durante la conversión RegisterRequest -> User")
    void shouldPreserveAllFieldsFromRegisterRequestToUser() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Ana")
                .lastName("Martínez")
                .email("ana.martinez@test.com")
                .password("Password123!")
                .build();

        User user = userMapper.toEntity(request);

        assertEquals(request.getFirstName(), user.getFirstName());
        assertEquals(request.getLastName(), user.getLastName());
        assertEquals(request.getEmail(), user.getEmail());
    }

    @Test
    @DisplayName("Debe preservar todos los campos visibles durante la conversión User -> UserResponse")
    void shouldPreserveAllFieldsFromUserToUserResponse() {
        User user = User.builder()
                .id(5L)
                .firstName("Luis")
                .lastName("Hernández")
                .email("luis.hernandez@test.com")
                .password("$2a$10$hashedPassword")
                .build();

        UserResponse response = userMapper.toResponse(user);

        assertEquals(user.getId(), response.getId());
        assertEquals(user.getFirstName(), response.getFirstName());
        assertEquals(user.getLastName(), response.getLastName());
        assertEquals(user.getEmail(), response.getEmail());
    }

    @Test
    @DisplayName("Debe crear objetos independientes (no compartir referencias)")
    void shouldCreateIndependentObjects() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("Jorge")
                .lastName("Fernández")
                .email("jorge.fernandez@test.com")
                .password("Password123!")
                .build();

        User user = userMapper.toEntity(request);
        
        // Modificar el request original
        request.setFirstName("Juan");

        // Then - El User no debe cambiar
        assertEquals("Jorge", user.getFirstName());
        assertNotEquals(request.getFirstName(), user.getFirstName());
    }

    @Test
    @DisplayName("Debe validar que UserResponse no exponga información sensible")
    void shouldNotExposePasswordInUserResponse() {
        User user = User.builder()
                .id(1L)
                .firstName("Test")
                .lastName("User")
                .email("test@test.com")
                .password("SuperSecretPassword123!")
                .build();

        UserResponse response = userMapper.toResponse(user);

        try {
            response.getClass().getDeclaredField("password");
            fail("UserResponse NO debería tener campo password");
        } catch (NoSuchFieldException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Debe manejar correctamente caracteres especiales en nombres")
    void shouldHandleSpecialCharactersInNames() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("José María")
                .lastName("García-López")
                .email("jose.garcia@test.com")
                .password("Password123!")
                .build();

        User user = userMapper.toEntity(request);
        UserResponse response = userMapper.toResponse(
                User.builder()
                        .id(1L)
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .email(user.getEmail())
                        .password("hashed")
                        .build()
        );

        assertEquals("José María", response.getFirstName());
        assertEquals("García-López", response.getLastName());
    }

    @Test
    @DisplayName("Debe manejar emails con diferentes formatos válidos")
    void shouldHandleVariousEmailFormats() {
        String[] validEmails = {
                "simple@test.com",
                "name.surname@company.co.uk",
                "user+tag@domain.com",
                "123456@test.com"
        };

        for (String email : validEmails) {
            RegisterRequest request = RegisterRequest.builder()
                    .firstName("Test")
                    .lastName("User")
                    .email(email)
                    .password("Password123!")
                    .build();

            User user = userMapper.toEntity(request);

            assertEquals(email, user.getEmail());
        }
    }
}
