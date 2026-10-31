package com.example.demo.controllers;

import com.example.demo.entities.CentroDistribucionEntity;
import com.example.demo.services.CentroDistribucionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/centrodistribucion")
@RequiredArgsConstructor
public class    CentroDistribucionController {
    private final CentroDistribucionService centroDistribucionService;


    @GetMapping
    public ResponseEntity<List<CentroDistribucionEntity>> findAll() {
        return ResponseEntity.ok(centroDistribucionService.findAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<CentroDistribucionEntity> findById(@PathVariable Integer id) {
        return  ResponseEntity.ok(centroDistribucionService.findById(id));
    }


    @PreAuthorize("hasRole('DESPACHADOR')")
    @PostMapping
    public ResponseEntity<CentroDistribucionEntity> saveCentroDistribucion(@jakarta.validation.Valid @RequestBody CentroDistribucionEntity centroDistribucionEntity) {
        CentroDistribucionEntity nuevoCentro =  centroDistribucionService.saveCentroDistribucion(centroDistribucionEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoCentro);
    }


    @PreAuthorize("hasRole('DESPACHADOR')")
    @PutMapping("/{id}")
    public ResponseEntity<CentroDistribucionEntity> updateCentroDistribucion(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody CentroDistribucionEntity centroDistribucionEntity
    ) {
        return ResponseEntity.ok(centroDistribucionService.updateCentroDistribucion(id, centroDistribucionEntity));
    }


    @PreAuthorize("hasRole('DESPACHADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<CentroDistribucionEntity> deleteCentroDistribucion(@PathVariable Integer id) {
        centroDistribucionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
