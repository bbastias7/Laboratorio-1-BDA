package com.example.demo.controllers;

import com.example.demo.entities.RepartidorEntity;
import com.example.demo.services.RepartidorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repartidor")
@RequiredArgsConstructor
public class RepartidorController {
    private final RepartidorService repartidorService;


    @GetMapping
    public ResponseEntity<List<RepartidorEntity>> findAll() {
        return ResponseEntity.ok(repartidorService.findAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<RepartidorEntity> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(repartidorService.findById(id));
    }


    @PostMapping
    public ResponseEntity<RepartidorEntity> saveRepartidor(@RequestBody RepartidorEntity repartidor) {
        RepartidorEntity nuevoRepartidor =  repartidorService.saveRepartidor(repartidor);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoRepartidor);
    }


    @PutMapping("/{id}")
    public ResponseEntity<RepartidorEntity> updateRepartidor(
            @PathVariable Integer id,
            @RequestBody RepartidorEntity repartidor
    ) {
        RepartidorEntity repartidorActualizado = repartidorService.updateRepartidor(id, repartidor);
        return ResponseEntity.ok(repartidorActualizado);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<RepartidorEntity> deleteRepartidor(@PathVariable Integer id) {
        repartidorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
