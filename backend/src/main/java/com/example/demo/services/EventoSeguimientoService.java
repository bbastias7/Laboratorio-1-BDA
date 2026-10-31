package com.example.demo.services;

import com.example.demo.entities.EventoSeguimientoEntity;
import com.example.demo.repositories.EventoSeguimientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class EventoSeguimientoService {
    private final EventoSeguimientoRepository eventoSeguimientoRepository;


    public List<EventoSeguimientoEntity> findAll(Integer paqueteId) {
        return eventoSeguimientoRepository.findAll();
    }


    public EventoSeguimientoEntity findById(Integer id) {
        return eventoSeguimientoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontro el registro"));
    }


    @Transactional
    public EventoSeguimientoEntity save(EventoSeguimientoEntity eventoSeguimientoEntity) {
        if(eventoSeguimientoEntity.getEventoId() != null && eventoSeguimientoRepository.findById(eventoSeguimientoEntity.getEventoId()).isPresent()) {
            throw new IllegalStateException("No se encontro el registro");
        }
        return eventoSeguimientoRepository.save(eventoSeguimientoEntity);
    }


    @Transactional
    public EventoSeguimientoEntity update(EventoSeguimientoEntity eventoSeguimientoEntity) {
        EventoSeguimientoEntity eventoExistente = findById(eventoSeguimientoEntity.getEventoId());
        eventoExistente.setPaqueteId(eventoSeguimientoEntity.getPaqueteId());
        eventoExistente.setEstado(eventoSeguimientoEntity.getEstado());
        eventoExistente.setDescripcion(eventoSeguimientoEntity.getDescripcion());
        eventoExistente.setFecha(eventoSeguimientoEntity.getFecha());
        int filasAfectadas = eventoSeguimientoRepository.update(eventoExistente);
        if(filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo actualizar el registro");
        }
        return eventoExistente;
    }


    @Transactional
    public void deleteById(Integer id) {
        int filasAfectadas = eventoSeguimientoRepository.deleteById(id);
        if(filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo eliminar el registro");
        }
    }
}
