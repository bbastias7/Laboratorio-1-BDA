package com.example.demo.controllers;
import com.example.demo.dto.Dtos.TrackingResponse;

import com.example.demo.dto.Dtos.DireccionRequest;
import com.example.demo.dto.Dtos.EntregaFallidaRequest;
import com.example.demo.dto.Dtos.EntregadoRequest;
import com.example.demo.entities.PaqueteEntity;
import com.example.demo.config.AuthUser;
import com.example.demo.services.PaqueteService;
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
@RequestMapping("/api")
@RequiredArgsConstructor
public class PaqueteController {

    private final PaqueteService paqueteService;

    @GetMapping("/paquete")
    public ResponseEntity<List<PaqueteEntity>> findAll(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required=false) String estado, @RequestParam(required=false) Integer centroId) {
        return ResponseEntity.ok(paqueteService.findVisibles(AuthUser.from(jwt), estado, centroId));
    }

    @GetMapping("/paquete/{id}")
    public ResponseEntity<PaqueteEntity> findById(@PathVariable Integer id, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paqueteService.findVisibleById(id, AuthUser.from(jwt)));
    }

    @PostMapping("/paquete")
    @PreAuthorize("hasAnyRole('CLIENTE','DESPACHADOR')")
    public ResponseEntity<PaqueteEntity> savePaquete(@jakarta.validation.Valid @RequestBody PaqueteEntity paqueteEntity,
                                                     @AuthenticationPrincipal Jwt jwt) {
        PaqueteEntity nuevoPaquete = paqueteService.registrar(paqueteEntity, AuthUser.from(jwt));
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoPaquete);
    }

    @PutMapping("/paquete/{id}")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<PaqueteEntity> updatePaquete(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody PaqueteEntity paqueteEntity,
            @AuthenticationPrincipal Jwt jwt
    ) {
        paqueteEntity.setPaqueteId(id);
        PaqueteEntity paqueteActualizado = paqueteService.updatePaquete(paqueteEntity, AuthUser.from(jwt));
        return ResponseEntity.ok(paqueteActualizado);
    }

    @DeleteMapping("/paquete/{id}")
    @PreAuthorize("hasRole('DESPACHADOR')")
    public ResponseEntity<Void> deletePaquete(@PathVariable Integer id) {
        paqueteService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/paquete/{id}/direccion")
    @PreAuthorize("hasAnyRole('CLIENTE','DESPACHADOR')")
    public ResponseEntity<PaqueteEntity> cambiarDireccion(@PathVariable Integer id,
                                                          @Valid @RequestBody DireccionRequest req,
                                                          @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paqueteService.cambiarDireccion(id, req.direccionDestino(), AuthUser.from(jwt)));
    }

    @PostMapping("/paquete/{id}/entrega-fallida")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<PaqueteEntity> entregaFallida(@PathVariable Integer id,
                                                        @Valid @RequestBody EntregaFallidaRequest req,
                                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paqueteService.registrarFallida(id, req.rutaId(), req.motivo(), AuthUser.from(jwt)));
    }

    @PatchMapping("/paquete/{id}/entregado")
    @PreAuthorize("hasRole('REPARTIDOR')")
    public ResponseEntity<PaqueteEntity> entregado(@PathVariable Integer id,
                                                   @Valid @RequestBody EntregadoRequest req,
                                                   @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paqueteService.registrarEntregado(id, req.rutaId(), AuthUser.from(jwt)));
    }
    @GetMapping("/tracking/{codigo}")
    public ResponseEntity<TrackingResponse> rastrear(
            @PathVariable String codigo, @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(paqueteService.rastrear(codigo, AuthUser.from(jwt)));
    }
}
