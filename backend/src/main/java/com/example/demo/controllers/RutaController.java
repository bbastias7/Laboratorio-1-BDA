package com.example.demo.controllers;

import com.example.demo.entities.RutaEntity;
import com.example.demo.services.RutaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ruta")
@RequiredArgsConstructor
public class RutaController {
    private final RutaService rutaService;


    @GetMapping
    public ResponseEntity<List<RutaEntity>> findAll() {
        return ResponseEntity.ok(rutaService.findAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<RutaEntity> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(rutaService.findById(id));
    }


    @PostMapping
    public ResponseEntity<RutaEntity> saveRuta(@RequestBody RutaEntity rutaEntity) {
        RutaEntity nuevaRuta =  rutaService.saveRuta(rutaEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRuta);
    }


    @PutMapping("/{id}")
    public ResponseEntity<RutaEntity> updateRuta(
            @PathVariable Integer id,
            @RequestBody RutaEntity rutaEntity
    ) {
        RutaEntity rutaActualizada = rutaService.updateRuta(id, rutaEntity);
        return ResponseEntity.ok(rutaActualizada);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<RutaEntity> deleteRuta(@PathVariable Integer id) {
        rutaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
