package com.example.demo.controllers;

import com.example.demo.entities.AuditoriaDireccionEntity;
import com.example.demo.services.AuditoriaDireccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('DESPACHADOR')")
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaDireccionController {
    private final AuditoriaDireccionService auditoriaDireccionService;


    @GetMapping
    public ResponseEntity<List<AuditoriaDireccionEntity>> findAll() {
        return ResponseEntity.ok(auditoriaDireccionService.findAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<AuditoriaDireccionEntity> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(auditoriaDireccionService.findById(id));
    }
}
