package com.example.demo.controllers;

import com.example.demo.entities.AuditoriaDireccionEntity;
import com.example.demo.services.AuditoriaDireccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
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


    @PostMapping
    public ResponseEntity<AuditoriaDireccionEntity> saveAuditoriaDireccion(@RequestBody AuditoriaDireccionEntity auditoriaDireccionEntity) {
        AuditoriaDireccionEntity nuevaAuditoria = auditoriaDireccionService.saveAuditoriaDireccion(auditoriaDireccionEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaAuditoria);
    }


    @PutMapping("/{id}")
    public ResponseEntity<AuditoriaDireccionEntity> updateAuditoriaDireccion(
            @PathVariable Integer id,
            @RequestBody AuditoriaDireccionEntity auditoriaDireccionEntity
    ) {
        auditoriaDireccionEntity.setAuditoriaId(id);
        AuditoriaDireccionEntity auditoriaActualizada = auditoriaDireccionService.updateAuditoriaDireccion(auditoriaDireccionEntity);
        return ResponseEntity.ok(auditoriaActualizada);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAuditoriaDireccion(@PathVariable Integer id) {
        auditoriaDireccionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
