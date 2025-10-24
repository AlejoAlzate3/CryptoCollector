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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            UserMapper userMapper,
            JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(
                    String.format(ErrorMessages.EMAIL_ALREADY_EXISTS, request.getEmail()));
        }

        // Mapear DTO a entidad
        User user = userMapper.toEntity(request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        return userMapper.toResponse(savedUser);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException(ErrorMessages.INVALID_CREDENTIALS));

        // Verificar contraseña
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(ErrorMessages.INVALID_CREDENTIALS);
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse(token);
    }
}
