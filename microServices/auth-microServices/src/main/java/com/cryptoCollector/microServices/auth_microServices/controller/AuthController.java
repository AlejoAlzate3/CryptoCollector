package com.cryptoCollector.microServices.auth_microServices.controller;

import com.cryptoCollector.microServices.auth_microServices.dto.*;
import com.cryptoCollector.microServices.auth_microServices.model.User;
import com.cryptoCollector.microServices.auth_microServices.service.UserService;
import com.cryptoCollector.microServices.auth_microServices.util.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        if (userService.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("El usuario ya existe");
        }
        User u = new User();
        u.setFirstName(req.getFirstName());
        u.setLastName(req.getLastName());
        u.setEmail(req.getEmail());
        u.setPassword(req.getPassword());
        User saved = userService.register(u);
        UserResponse resp = new UserResponse(saved.getId(), saved.getFirstName(), saved.getLastName(),
                saved.getEmail());
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var opt = userService.findByEmail(req.getEmail());
        if (opt.isEmpty())
            return ResponseEntity.status(401).body("Correo y/o contraseña inválidos");
        var user = opt.get();
        if (!userService.checkPassword(req.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Correo y/o contraseña inválidos");
        }
        String token = jwtUtil.generateToken(user.getEmail());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
