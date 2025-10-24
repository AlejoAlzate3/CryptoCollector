package com.cryptoCollector.microServices.auth_microServices.service;

import com.cryptoCollector.microServices.auth_microServices.constants.ErrorMessages;
import com.cryptoCollector.microServices.auth_microServices.dto.AuthResponse;
import com.cryptoCollector.microServices.auth_microServices.dto.LoginRequest;
import com.cryptoCollector.microServices.auth_microServices.dto.RegisterRequest;
import com.cryptoCollector.microServices.auth_microServices.dto.UserResponse;
import com.cryptoCollector.microServices.auth_microServices.exception.ConflictException;
import com.cryptoCollector.microServices.auth_microServices.exception.InvalidCredentialsException;
import com.cryptoCollector.microServices.auth_microServices.mapper.UserMapper;
import com.cryptoCollector.microServices.auth_microServices.model.User;
import com.cryptoCollector.microServices.auth_microServices.repository.UserRepository;
import com.cryptoCollector.microServices.auth_microServices.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests Unitarios")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest validRegisterRequest;
    private LoginRequest validLoginRequest;
    private User testUser;
    private UserResponse testUserResponse;

    @BeforeEach
    void setUp() {
        validRegisterRequest = RegisterRequest.builder()
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@test.com")
                .password("TestPassword123!")
                .build();

        validLoginRequest = LoginRequest.builder()
                .email("juan.perez@test.com")
                .password("TestPassword123!")
                .build();

        testUser = User.builder()
                .id(1L)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@test.com")
                .password("$2a$10$encodedPassword")
                .build();

        testUserResponse = UserResponse.builder()
                .id(1L)
                .firstName("Juan")
                .lastName("Pérez")
                .email("juan.perez@test.com")
                .build();
    }

    @Test
    @DisplayName("Debe registrar usuario exitosamente cuando el email no existe")
    void shouldRegisterUserSuccessfully() {
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(userMapper.toEntity(validRegisterRequest)).thenReturn(testUser);
        when(passwordEncoder.encode(validRegisterRequest.getPassword())).thenReturn("$2a$10$encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        UserResponse response = authService.register(validRegisterRequest);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Juan", response.getFirstName());
        assertEquals("Pérez", response.getLastName());
        assertEquals("juan.perez@test.com", response.getEmail());

        verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        verify(userMapper).toEntity(validRegisterRequest);
        verify(passwordEncoder).encode(validRegisterRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(userMapper).toResponse(testUser);
    }

    @Test
    @DisplayName("Debe lanzar ConflictException cuando el email ya existe")
    void shouldThrowConflictExceptionWhenEmailExists() {
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> authService.register(validRegisterRequest));

        assertTrue(exception.getMessage().contains(validRegisterRequest.getEmail()));
        assertTrue(exception.getMessage().contains("ya está registrado"));

        verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        verify(userRepository, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Debe hacer login exitosamente con credenciales correctas")
    void shouldLoginSuccessfullyWithValidCredentials() {
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";

        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(validLoginRequest.getPassword(), testUser.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(testUser.getEmail())).thenReturn(expectedToken);

        AuthResponse response = authService.login(validLoginRequest);

        assertNotNull(response);
        assertEquals(expectedToken, response.getToken());

        verify(userRepository).findByEmail(validLoginRequest.getEmail());
        verify(passwordEncoder).matches(validLoginRequest.getPassword(), testUser.getPassword());
        verify(jwtUtil).generateToken(testUser.getEmail());
    }

    @Test
    @DisplayName("Debe lanzar InvalidCredentialsException cuando el usuario no existe")
    void shouldThrowInvalidCredentialsExceptionWhenUserNotFound() {
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.empty());

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(validLoginRequest));

        assertEquals(ErrorMessages.INVALID_CREDENTIALS, exception.getMessage());

        verify(userRepository).findByEmail(validLoginRequest.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Debe lanzar InvalidCredentialsException cuando la contraseña es incorrecta")
    void shouldThrowInvalidCredentialsExceptionWhenPasswordIsWrong() {
        when(userRepository.findByEmail(validLoginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(validLoginRequest.getPassword(), testUser.getPassword())).thenReturn(false);

        InvalidCredentialsException exception = assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(validLoginRequest));

        assertEquals(ErrorMessages.INVALID_CREDENTIALS, exception.getMessage());

        verify(userRepository).findByEmail(validLoginRequest.getEmail());
        verify(passwordEncoder).matches(validLoginRequest.getPassword(), testUser.getPassword());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    @DisplayName("Debe encriptar la contraseña antes de guardar el usuario")
    void shouldEncryptPasswordBeforeSavingUser() {
        String rawPassword = "PlainTextPassword123!";
        String encodedPassword = "$2a$10$hashdelacontraseña";

        RegisterRequest request = RegisterRequest.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@test.com")
                .password(rawPassword)
                .build();

        User userBeforeEncoding = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@test.com")
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(userBeforeEncoding);
        when(passwordEncoder.encode(rawPassword)).thenReturn(encodedPassword);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(any(User.class))).thenReturn(testUserResponse);

        authService.register(request);

        verify(passwordEncoder).encode(rawPassword);

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Debe validar que el email no exista antes de cualquier operación de registro")
    void shouldCheckEmailExistenceBeforeAnyRegisterOperation() {
        when(userRepository.existsByEmail(validRegisterRequest.getEmail())).thenReturn(false);
        when(userMapper.toEntity(validRegisterRequest)).thenReturn(testUser);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userMapper.toResponse(testUser)).thenReturn(testUserResponse);

        authService.register(validRegisterRequest);

        var inOrder = inOrder(userRepository, userMapper, passwordEncoder);
        inOrder.verify(userRepository).existsByEmail(validRegisterRequest.getEmail());
        inOrder.verify(userMapper).toEntity(validRegisterRequest);
        inOrder.verify(passwordEncoder).encode(anyString());
        inOrder.verify(userRepository).save(any(User.class));
    }
}
