package com.example.demo.controllers;

import com.example.demo.entities.EventoSeguimientoEntity;
import com.example.demo.services.EventoSeguimientoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@PreAuthorize("hasRole('DESPACHADOR')")
@RequestMapping("/api/eventoseguimiento")
@RequiredArgsConstructor
public class EventoSeguimientoController {
    private final EventoSeguimientoService eventoSeguimientoService;


    @GetMapping
    public ResponseEntity<List<EventoSeguimientoEntity>> findAll() {
        return ResponseEntity.ok(eventoSeguimientoService.findAll(null));
    }


    @GetMapping("/{id}")
    public ResponseEntity<EventoSeguimientoEntity> findById(@PathVariable Integer id) {
        return ResponseEntity.ok(eventoSeguimientoService.findById(id));
    }


    @PostMapping
    public ResponseEntity<EventoSeguimientoEntity> saveEventoSeguimiento(@jakarta.validation.Valid @RequestBody EventoSeguimientoEntity eventoSeguimientoEntity) {
        EventoSeguimientoEntity nuevoEvento = eventoSeguimientoService.save(eventoSeguimientoEntity);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoEvento);
    }


    @PutMapping("/{id}")
    public ResponseEntity<EventoSeguimientoEntity> updateEventoSeguimiento(
            @PathVariable Integer id,
            @jakarta.validation.Valid @RequestBody EventoSeguimientoEntity eventoSeguimientoEntity
    ) {
        eventoSeguimientoEntity.setEventoId(id);
        EventoSeguimientoEntity eventoActualizado = eventoSeguimientoService.update(eventoSeguimientoEntity);
        return ResponseEntity.ok(eventoActualizado);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEventoSeguimiento(@PathVariable Integer id) {
        eventoSeguimientoService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
