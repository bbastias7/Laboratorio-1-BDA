package com.example.demo.controllers;

import com.example.demo.dto.Dtos.AsignarRutaRequest;
import com.example.demo.entities.RutaEntity;
import com.example.demo.config.AuthUser;
import com.example.demo.services.RutaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ruta")
@RequiredArgsConstructor
public class RutaController {

    private final RutaService rutaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('DESPACHADOR','REPARTIDOR')")
    public ResponseEntity<List<RutaEntity>> findAll(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(rutaService.findVisibles(AuthUser.from(jwt)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DESPACHADOR','REPARTIDOR')")
    public ResponseEntity<RutaEntity> findById(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(rutaService.findVisibleById(id, AuthUser.from(jwt)));
    }

    @PostMapping
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<RutaEntity> saveRuta(@jakarta.validation.Valid @RequestBody RutaEntity rutaEntity) {
        RutaEntity nuevaRuta =  rutaService.saveRuta(rutaEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevaRuta);
    }

    @PostMapping("/asignar")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<RutaEntity> asignar(@Valid @RequestBody AsignarRutaRequest req) {
        RutaEntity ruta = rutaService.asignar(req.centroDistId(), req.repartidorId(), req.vehiculoId(),
                req.fecha(), req.paqueteIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ruta);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<RutaEntity> updateRuta(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody RutaEntity rutaEntity
    ) {
        RutaEntity rutaActualizada = rutaService.updateRuta(id, rutaEntity);
        return ResponseEntity.ok(rutaActualizada);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<RutaEntity> deleteRuta(@PathVariable Integer id) {
        rutaService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
