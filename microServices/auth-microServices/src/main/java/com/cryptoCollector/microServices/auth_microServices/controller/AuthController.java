package com.cryptoCollector.microServices.auth_microServices.controller;

import com.cryptoCollector.microServices.auth_microServices.dto.*;
import com.cryptoCollector.microServices.auth_microServices.exception.ConflictException;
import com.cryptoCollector.microServices.auth_microServices.exception.InvalidCredentialsException;
import com.cryptoCollector.microServices.auth_microServices.model.User;
import com.cryptoCollector.microServices.auth_microServices.service.UserService;
import com.cryptoCollector.microServices.auth_microServices.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para registro y login de usuarios")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @Operation(summary = "Registrar nuevo usuario", 
               description = "Crea un nuevo usuario en el sistema. El email debe ser único.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente",
                     content = @Content(schema = @Schema(implementation = UserResponse.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "El email ya está registrado",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        // Verificar si el email ya existe
        if (userService.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("El email '" + req.getEmail() + "' ya está registrado");
        }
        
        // Crear y guardar el nuevo usuario
        User u = new User();
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setEmail(req.getEmail());
        u.setPassword(req.getPassword());
        User saved = userService.register(u);
        
        // Retornar respuesta con datos del usuario
        UserResponse resp = new UserResponse(
                saved.getId(), 
                saved.getFirstName(), 
                saved.getLastName(),
                saved.getEmail()
        );
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "Iniciar sesión", 
               description = "Autentica un usuario y retorna un token JWT válido por 24 horas.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Login exitoso, token JWT retornado",
                     content = @Content(schema = @Schema(implementation = AuthResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas",
                     content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        // Buscar usuario por email
        var opt = userService.findByEmail(req.getEmail());
        if (opt.isEmpty()) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }
        
        var user = opt.get();
        
        // Verificar contraseña
        if (!userService.checkPassword(req.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Credenciales inválidas");
        }
        
        // Generar y retornar token JWT
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
