package com.example.demo.services;

import com.example.demo.entities.RepartidorEntity;
import com.example.demo.repositories.RepartidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RepartidorService {
    private final RepartidorRepository repartidorRepository;


    public List<RepartidorEntity> findAll() {
        return repartidorRepository.findAll();
    }


    public RepartidorEntity findById(int id) {
        return repartidorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Repartidor inexistente"));
    }


    public RepartidorEntity saveRepartidor(RepartidorEntity repartidor) {
        if (repartidorRepository.findById(repartidor.getRepartidorId()).isPresent()) {
            throw new IllegalArgumentException("El repartidor ya existe en la plataforma");
        }
        return repartidorRepository.save(repartidor);
    }


    public RepartidorEntity updateRepartidor(Integer id, RepartidorEntity repartidor) {
        RepartidorEntity repartidorExistente = findById(id);
        repartidorExistente.setUsuarioId(repartidor.getUsuarioId());
        repartidorExistente.setCentroDistId(repartidor.getCentroDistId());
        int filasAfectadas = repartidorRepository.update(repartidorExistente);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo actualizar el repartidor");
        }
        return repartidorExistente;
    }


    public void deleteById(int id) {
        findById(id);
        int filasAfectadas = repartidorRepository.deleteById(id);
        if(filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo eliminar el repartidor");
        }
    }
}
