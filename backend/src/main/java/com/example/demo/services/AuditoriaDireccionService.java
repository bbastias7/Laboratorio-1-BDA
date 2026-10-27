package com.example.demo.services;

import com.example.demo.entities.AuditoriaDireccionEntity;
import com.example.demo.repositories.AuditoriaDireccionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class AuditoriaDireccionService {
    private final AuditoriaDireccionRepository auditoriaDireccionRepository;


    public List<AuditoriaDireccionEntity> findAll() {
        return auditoriaDireccionRepository.findAll();
    }


    public AuditoriaDireccionEntity findById(Integer id) {
        return auditoriaDireccionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontro el registro"));
    }


    public AuditoriaDireccionEntity saveAuditoriaDireccion(AuditoriaDireccionEntity auditoriaDireccionEntity) {
        if(auditoriaDireccionRepository.findById(auditoriaDireccionEntity.getAuditoriaId()).isPresent()) {
            throw new NoSuchElementException("No se encontro el registro");
        }
        return auditoriaDireccionRepository.save(auditoriaDireccionEntity);
    }


    public AuditoriaDireccionEntity updateAuditoriaDireccion(AuditoriaDireccionEntity auditoriaDireccionEntity) {
        AuditoriaDireccionEntity auditoriaExistente = findById(auditoriaDireccionEntity.getAuditoriaId());
        auditoriaExistente.setPaqueteId(auditoriaDireccionEntity.getPaqueteId());
        auditoriaExistente.setDireccionAnterior(auditoriaDireccionEntity.getDireccionAnterior());
        auditoriaExistente.setDireccionNueva(auditoriaDireccionEntity.getDireccionNueva());
        auditoriaExistente.setUsuarioId(auditoriaDireccionEntity.getUsuarioId());
        auditoriaExistente.setFecha(auditoriaDireccionEntity.getFecha());
        int filasAfectadas = auditoriaDireccionRepository.update(auditoriaExistente);
        if(filasAfectadas == 0) {
            throw new NoSuchElementException("No se encontro el registro");
        }
        return auditoriaExistente;
    }


    public void deleteById(Integer id) {
        int filasAfectadas = auditoriaDireccionRepository.deleteById(id);
        if(filasAfectadas == 0) {
            throw new NoSuchElementException("No se encontro el registro");
        }
    }
}
