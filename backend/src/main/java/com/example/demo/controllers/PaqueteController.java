package com.example.demo.controllers;

import com.example.demo.entities.PaqueteEntity;
import com.example.demo.services.PaqueteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paquete")
@RequiredArgsConstructor
public class PaqueteController {
    private final PaqueteService paqueteService;


    @GetMapping
    public ResponseEntity<List<PaqueteEntity>> findAll() {
        return ResponseEntity.ok(paqueteService.findAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<PaqueteEntity> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(paqueteService.findById(id));
    }


    @PostMapping
    public ResponseEntity<PaqueteEntity> savePaquete(@RequestBody PaqueteEntity paqueteEntity) {
        PaqueteEntity nuevoPaquete = paqueteService.savePaquete(paqueteEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPaquete);
    }


    @PutMapping("/{id}")
    public ResponseEntity<PaqueteEntity> updatePaquete(
            @PathVariable Integer id,
            @RequestBody PaqueteEntity paqueteEntity
    ) {
        paqueteEntity.setPaqueteId(id);
        PaqueteEntity paqueteActualizado = paqueteService.updatePaquete(paqueteEntity);
        return ResponseEntity.ok(paqueteActualizado);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaquete(@PathVariable Integer id) {
        paqueteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
