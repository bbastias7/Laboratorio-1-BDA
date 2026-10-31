package com.example.demo.controllers;
import com.example.demo.dto.Dtos.EntregaView;
import com.example.demo.config.AuthUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import com.example.demo.entities.RepartidorEntity;
import com.example.demo.services.RepartidorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('DESPACHADOR')")
@RequestMapping("/api")
@RequiredArgsConstructor
public class RepartidorController {
    private final RepartidorService repartidorService;


    @GetMapping("/repartidor")
    public ResponseEntity<List<RepartidorEntity>> findAll() {
        return ResponseEntity.ok(repartidorService.findAll());
    }


    @GetMapping("/repartidor/{id}")
    public ResponseEntity<RepartidorEntity> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(repartidorService.findById(id));
    }


    @PostMapping("/repartidor")
    public ResponseEntity<RepartidorEntity> saveRepartidor(@jakarta.validation.Valid @RequestBody RepartidorEntity repartidor) {
        RepartidorEntity nuevoRepartidor =  repartidorService.saveRepartidor(repartidor);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoRepartidor);
    }


    @PutMapping("/repartidor/{id}")
    public ResponseEntity<RepartidorEntity> updateRepartidor(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody RepartidorEntity repartidor
    ) {
        RepartidorEntity repartidorActualizado = repartidorService.updateRepartidor(id, repartidor);
        return ResponseEntity.ok(repartidorActualizado);
    }


    @DeleteMapping("/repartidor/{id}")
    public ResponseEntity<RepartidorEntity> deleteRepartidor(@PathVariable Integer id) {
        repartidorService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/entregas")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<List<EntregaView>> entregas(
            @RequestParam(defaultValue="false") boolean soloPendientes,
            @AuthenticationPrincipal
            Jwt jwt) {
        return ResponseEntity.ok(repartidorService.listarEntregas(
                AuthUser.from(jwt), soloPendientes));
    }
}
