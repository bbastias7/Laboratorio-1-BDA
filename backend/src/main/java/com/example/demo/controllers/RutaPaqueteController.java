package com.example.demo.controllers;

import com.example.demo.entities.RutaPaqueteEntity;
import com.example.demo.services.RutaPaqueteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('DESPACHADOR')")
@RequestMapping("/api/rutapaquete")
@RequiredArgsConstructor
public class RutaPaqueteController {
    private final RutaPaqueteService rutaPaqueteService;


    @GetMapping
    public ResponseEntity<List<RutaPaqueteEntity>> findAll() {
        return ResponseEntity.ok(rutaPaqueteService.findAll());
    }


    @GetMapping("/{rutaId}/{paqueteId}")
    public ResponseEntity<RutaPaqueteEntity> findById(
            @PathVariable Integer rutaId,
            @PathVariable Integer paqueteId
    ) {
        return ResponseEntity.ok(rutaPaqueteService.findById(rutaId, paqueteId));
    }


    @PostMapping
    public ResponseEntity<RutaPaqueteEntity> saveRutaPaquete(@jakarta.validation.Valid @RequestBody RutaPaqueteEntity rutaPaqueteEntity) {
        RutaPaqueteEntity nuevaRutaPaquete = rutaPaqueteService.saveRutaPaquete(rutaPaqueteEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRutaPaquete);
    }


    @PutMapping("/{rutaId}/{paqueteId}")
    public ResponseEntity<RutaPaqueteEntity> updateRutaPaquete(
            @PathVariable Integer rutaId,
            @PathVariable Integer paqueteId,
            @jakarta.validation.Valid @RequestBody RutaPaqueteEntity rutaPaqueteEntity
    ) {
        rutaPaqueteEntity.setRutaId(rutaId);
        rutaPaqueteEntity.setPaqueteId(paqueteId);
        RutaPaqueteEntity rutaPaqueteActualizada = rutaPaqueteService.updateRutaPaquete(rutaPaqueteEntity);
        return ResponseEntity.ok(rutaPaqueteActualizada);
    }


    @DeleteMapping("/{rutaId}/{paqueteId}")
    public ResponseEntity<Void> deleteRutaPaquete(
            @PathVariable Integer rutaId,
            @PathVariable Integer paqueteId
    ) {
        rutaPaqueteService.deleteById(rutaId, paqueteId);
        return ResponseEntity.noContent().build();
    }
}
