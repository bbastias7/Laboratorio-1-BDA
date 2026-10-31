package com.example.demo.controllers;

import com.example.demo.entities.VehiculoEntity;
import com.example.demo.services.VehiculoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('DESPACHADOR')")
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {
    private final VehiculoService vehiculoService;


    @GetMapping
    public ResponseEntity<List<VehiculoEntity>> findAll() {
        return ResponseEntity.ok(vehiculoService.findAll());
    }


    @GetMapping("/{id}")
    public ResponseEntity<VehiculoEntity> findById(@PathVariable Integer id) {
        VehiculoEntity vehiculo = vehiculoService.findById(id);
        return ResponseEntity.ok(vehiculo);
    }


    @PostMapping
    public ResponseEntity<VehiculoEntity> saveVehiculo(@jakarta.validation.Valid @RequestBody VehiculoEntity vehiculo) {
        VehiculoEntity nuevoVehiculo = vehiculoService.saveVehiculo(vehiculo);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoVehiculo);
    }


    @PutMapping("/{id}")
    public ResponseEntity<VehiculoEntity> updateVehiculo(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody VehiculoEntity vehiculo) {
        VehiculoEntity vehiculoActualizado = vehiculoService.updateVehiculo(id, vehiculo);
        return ResponseEntity.ok(vehiculoActualizado);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehiculo(@PathVariable Integer id) {
        vehiculoService.deleteVehiculo(id);
        return ResponseEntity.noContent().build();
    }
}
