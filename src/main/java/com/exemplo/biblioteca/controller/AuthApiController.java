package com.exemplo.biblioteca.controller;

import com.exemplo.biblioteca.dto.LoginRequest;
import com.exemplo.biblioteca.dto.LoginResponse;
import com.exemplo.biblioteca.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthApiController(AuthenticationConfiguration configuration, JwtService jwtService) throws Exception {
        this.authenticationManager = configuration.getAuthenticationManager();
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );
        return ResponseEntity.ok(new LoginResponse(jwtService.gerarToken(request.username()), "Bearer"));
    }
}
