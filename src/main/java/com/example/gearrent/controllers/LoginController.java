package com.example.gearrent.controllers;

import com.example.gearrent.DTO.LoginRequest;
import com.example.gearrent.DTO.LoginResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/login")
public class LoginController {

    @PostMapping
    public ResponseEntity<LoginResponse> logar(@RequestBody LoginRequest loginRequest) {

        if (loginRequest.login().equals("string") && loginRequest.senha().equals("string")) {
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setMensagem("Bem Vindo");
            return ResponseEntity.ok(loginResponse);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
