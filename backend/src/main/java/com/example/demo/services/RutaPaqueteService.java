package com.example.demo.services;

import com.example.demo.entities.RutaPaqueteEntity;
import com.example.demo.repositories.RutaPaqueteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RutaPaqueteService {
    private final RutaPaqueteRepository rutaPaqueteRepository;


    public List<RutaPaqueteEntity> findAll() {
        return rutaPaqueteRepository.findAll();
    }


    public RutaPaqueteEntity findById(Integer rutaId, Integer paqueteId) {
        return rutaPaqueteRepository.findByIds(rutaId, paqueteId)
                .orElseThrow(() -> new NoSuchElementException("Relación no encontrada"));
    }


    public RutaPaqueteEntity saveRutaPaquete(RutaPaqueteEntity rutaPaquete) {
        return rutaPaqueteRepository.save(rutaPaquete);
    }


    public RutaPaqueteEntity updateRutaPaquete(RutaPaqueteEntity rutaPaquete) {
        RutaPaqueteEntity rutaPaqueteExistente = findById(rutaPaquete.getRutaId(), rutaPaquete.getPaqueteId());
        rutaPaqueteExistente.setResultado(rutaPaquete.getResultado());
        rutaPaqueteExistente.setFechaResultado(rutaPaquete.getFechaResultado());
        int filasAfectadas = rutaPaqueteRepository.update(rutaPaqueteExistente);
        if(filasAfectadas == 0){
            throw new IllegalStateException("No se puede actualizar el paquete");
        }
        return rutaPaqueteExistente;
    }


    public void deleteById(Integer rutaId, Integer paqueteId) {
        rutaPaqueteRepository.deleteByIds(rutaId, paqueteId);
    }
}
