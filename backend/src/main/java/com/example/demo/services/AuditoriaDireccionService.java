package com.example.demo.services;

import com.example.demo.dto.Pagina;
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


    public Pagina<AuditoriaDireccionEntity> findPage(Integer paqueteId, Integer usuarioId, Integer page, Integer size) {
        return auditoriaDireccionRepository.findPage(paqueteId, usuarioId, page, size);
    }


    public AuditoriaDireccionEntity findById(Integer id) {
        return auditoriaDireccionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontro el registro"));
    }
}
