package com.zonapos.controller;

import com.zonapos.dto.AuthResponse;
import com.zonapos.dto.LoginRequest;
import com.zonapos.dto.RegisterTenantRequest;
import com.zonapos.dto.UserDto;
import com.zonapos.entity.User;
import com.zonapos.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);

        // Also set cookie
        Cookie cookie = new Cookie("zona_pos_token", authResponse.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (authResponse.getExpiresIn() / 1000));
        response.addCookie(cookie);

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register-tenant")
    public ResponseEntity<AuthResponse> registerTenant(@Valid @RequestBody RegisterTenantRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.registerTenant(request);

        Cookie cookie = new Cookie("zona_pos_token", authResponse.getToken());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (authResponse.getExpiresIn() / 1000));
        response.addCookie(cookie);

        return ResponseEntity.ok(authResponse);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.mapToUserDto(user));
    }
}
