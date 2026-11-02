package com.example.demo.controllers;

import com.example.demo.dto.Dtos.RegistroRequest;
import com.example.demo.entities.UsuarioEntity;
import com.example.demo.services.RegistroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public endpoints (permitAll in SecurityConfig). */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegistroService registroService;

    @PostMapping("/registro")
    public ResponseEntity<UsuarioEntity> registro(@Valid @RequestBody RegistroRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registroService.registrar(req));
    }
}
