package com.example.demo.services;

import com.example.demo.entities.CentroDistribucionEntity;
import com.example.demo.repositories.CentroDistribucionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CentroDistribucionService {
    private final CentroDistribucionRepository centroDistribucionRepository;


    public List<CentroDistribucionEntity> findAll() {
        return centroDistribucionRepository.findAll();
    }


    public CentroDistribucionEntity findById(int id) {
        return centroDistribucionRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No se encontro el centro de distribucion"));
    }


    @Transactional
    public CentroDistribucionEntity saveCentroDistribucion(CentroDistribucionEntity centroDistribucion) {
        if(centroDistribucion.getCentroDistId() != null && centroDistribucionRepository.findById(centroDistribucion.getCentroDistId()).isPresent()) {
            throw new IllegalStateException("El centro distribucion existe en el sistema");
        }
        return centroDistribucionRepository.save(centroDistribucion);
    }


    @Transactional
    public CentroDistribucionEntity updateCentroDistribucion(Integer id, CentroDistribucionEntity centroDistribucion) {
        CentroDistribucionEntity centroExistente = findById(id);
        centroExistente.setNombre(centroDistribucion.getNombre());
        centroExistente.setDireccion(String.valueOf(centroDistribucion.getDireccion()));
        centroExistente.setCiudad(String.valueOf(centroDistribucion.getCiudad()));
        int filasAfectadas = centroDistribucionRepository.update(centroExistente);
        if(filasAfectadas == 0) {
            throw new IllegalStateException("No se encontro el centro distribucion");
        }
        return centroExistente;
    }


    @Transactional
    public void deleteById(int id) {
        int filasAfectadas = centroDistribucionRepository.deleteById(id);
        if(filasAfectadas == 0) {
            throw new NoSuchElementException("No se pudo eliminar el centro distribucion");
        }
    }
}
