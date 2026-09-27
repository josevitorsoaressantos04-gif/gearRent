package com.example.gearrent.controllers;

import com.example.gearrent.DTO.LoginRequest;
import com.example.gearrent.DTO.LoginResponse;
import com.example.gearrent.service.LoginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {

    private final LoginService loginService;

    // Injeção de dependência nativa pelo construtor
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping
    public ResponseEntity<LoginResponse> autenticar(@RequestBody LoginRequest request) {

        LoginResponse response = loginService.autenticar(request);

        return ResponseEntity.ok(response);
    }
}