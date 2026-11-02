package com.example.demo.services;

import com.example.demo.dto.Pagina;
import com.example.demo.entities.RutaPaqueteEntity;
import com.example.demo.repositories.PaqueteRepository;
import com.example.demo.repositories.RutaPaqueteRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RutaPaqueteService {
    private final RutaPaqueteRepository rutaPaqueteRepository;
    private final PaqueteRepository paqueteRepository;

    public List<RutaPaqueteEntity> findAll() {
        return rutaPaqueteRepository.findAll();
    }

    public Pagina<RutaPaqueteEntity> findPage(Integer rutaId, Integer paqueteId, String resultado,
                                              Integer page, Integer size) {
        if (resultado != null && !resultado.isBlank()
                && !java.util.Set.of("Pendiente", "Entregado", "Fallido").contains(resultado))
            throw new IllegalArgumentException("Resultado no válido");
        return rutaPaqueteRepository.findPage(rutaId, paqueteId, resultado, page, size);
    }

    public RutaPaqueteEntity findById(Integer rutaId, Integer paqueteId) {
        return rutaPaqueteRepository.findByIds(rutaId, paqueteId)
                .orElseThrow(() -> new NoSuchElementException("Asignación inexistente"));
    }

    @Transactional
    public RutaPaqueteEntity saveRutaPaquete(RutaPaqueteEntity asignacion) {
        if (asignacion.getRutaId() == null || asignacion.getPaqueteId() == null) {
            throw new IllegalArgumentException("rutaId y paqueteId requeridos");
        }
        rutaPaqueteRepository.agregar(asignacion.getRutaId(), asignacion.getPaqueteId());
        return findById(asignacion.getRutaId(), asignacion.getPaqueteId());
    }

    @Transactional
    public RutaPaqueteEntity updateRutaPaquete(RutaPaqueteEntity asignacion) {
        findById(asignacion.getRutaId(), asignacion.getPaqueteId());
        switch (Objects.toString(asignacion.getResultado(), "")) {
            case "Entregado" -> paqueteRepository.registrarExitosa(
                    asignacion.getPaqueteId(), asignacion.getRutaId());
            case "Fallido" -> paqueteRepository.registrarFallida(
                    asignacion.getPaqueteId(), asignacion.getRutaId(), "Resultado administrativo");
            default -> throw new IllegalArgumentException(
                    "Resultado debe ser Entregado o Fallido; no se reabren asignaciones");
        }
        return findById(asignacion.getRutaId(), asignacion.getPaqueteId());
    }

    @Transactional
    public void deleteById(Integer rutaId, Integer paqueteId) {
        findById(rutaId, paqueteId);
        rutaPaqueteRepository.quitar(rutaId, paqueteId);
    }
}
