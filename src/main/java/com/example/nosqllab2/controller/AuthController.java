package com.example.nosqllab2.controller;

import com.example.nosqllab2.dto.request.auth.LoginDto;
import com.example.nosqllab2.dto.request.auth.RegisterDto;
import com.example.nosqllab2.dto.response.AuthTokenDto;
import com.example.nosqllab2.dto.response.UserResponseDto;
import com.example.nosqllab2.security.AuthenticatedUser;
import com.example.nosqllab2.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthTokenDto> login(@Valid @RequestBody LoginDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthTokenDto> register(@Valid @RequestBody RegisterDto dto) {
        AuthTokenDto token = authService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.ok(authService.getCurrentUser(user.id()));
    }
}
