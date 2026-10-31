package com.example.demo.controllers;

import com.example.demo.dto.Dtos.Alertas;
import com.example.demo.dto.Dtos.Rendimiento;
import com.example.demo.services.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes")
@PreAuthorize("hasRole('DESPACHADOR')")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/rendimiento")
    public ResponseEntity<List<Rendimiento>> rendimiento() {
        return ResponseEntity.ok(reporteService.rendimiento());
    }

    @GetMapping("/alertas")
    public ResponseEntity<Alertas> alertas(@RequestParam(defaultValue = "85") int umbral) {
        return ResponseEntity.ok(reporteService.alertas(umbral));
    }

    @PostMapping("/refrescar")
    public ResponseEntity<Map<String, LocalDateTime>> refrescar() {
        return ResponseEntity.ok(reporteService.refrescar());
    }
}
