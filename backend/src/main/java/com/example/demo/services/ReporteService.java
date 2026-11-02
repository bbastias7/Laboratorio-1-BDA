package com.example.demo.services;

import com.example.demo.dto.Dtos.Alertas;
import com.example.demo.dto.Dtos.Rendimiento;
import com.example.demo.repositories.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Servicio de reportes de rendimiento.
 * <p>Consulta la vista materializada {@code mv_rendimiento_entregas},
 * genera alertas de desempeño bajo un umbral configurable y gestiona
 * el refresco programado de la vista.</p>
 */
@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final Logger log = LoggerFactory.getLogger(ReporteService.class);

    private final ReporteRepository reporteRepository;

    public List<Rendimiento> rendimiento() {
        return reporteRepository.rendimiento();
    }

    public Alertas alertas(int umbral) {
        if (umbral < 0 || umbral > 100) throw new IllegalArgumentException("Umbral entre 0 y 100");
        return new Alertas(umbral, reporteRepository.repartidoresBajo(umbral), reporteRepository.centrosBajo(umbral));
    }

    public Map<String, LocalDateTime> refrescar() {
        reporteRepository.refresh();
        return Map.of("refrescadoEn", LocalDateTime.now().withNano(0));
    }

    @Scheduled(cron = "${REFRESH_CRON:0 0 3 * * *}", zone = "America/Santiago")
    void refrescoProgramado() {
        try {
            reporteRepository.refresh();
            log.info("mv_rendimiento_entregas refreshed by schedule");
        } catch (RuntimeException e) {
            log.error("Scheduled refresh of mv_rendimiento_entregas failed", e);
        }
    }
}
