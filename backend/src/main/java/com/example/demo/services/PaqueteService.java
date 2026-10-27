package com.example.demo.services;

import com.example.demo.entities.PaqueteEntity;
import com.example.demo.entities.RepartidorEntity;
import com.example.demo.repositories.PaqueteRepository;
import com.example.demo.repositories.RepartidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class PaqueteService {
    private final PaqueteRepository paqueteRepository;


    public List<PaqueteEntity> findAll() {
        return paqueteRepository.findAll();
    }


    public PaqueteEntity findById(Integer id) {
        return paqueteRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Paquete inexistente"));
    }


    public PaqueteEntity findByCodigo(String codigo) {
        return paqueteRepository.findByCodigo(codigo)
                .orElseThrow(() -> new NoSuchElementException("Paquete inexistente"));
    }

    /*
    public List<PaqueteEntity> findByEstado(String estado) {
        return paqueteRepository.findByEstado(estado),
    }
    */

    public PaqueteEntity savePaquete(PaqueteEntity paqueteEntity) {
        if(paqueteRepository.findByCodigo(paqueteEntity.getCodigo()).isPresent()) {
            throw new IllegalStateException("Paquete ya existe");
        }
        return paqueteRepository.save(paqueteEntity);
    }


    public PaqueteEntity updatePaquete(PaqueteEntity paqueteEntity) {
        PaqueteEntity paqueteExistente = findById(paqueteEntity.getPaqueteId());
        paqueteExistente.setCodigo(paqueteEntity.getCodigo());
        paqueteExistente.setEstado(paqueteEntity.getEstado());
        paqueteExistente.setPeso(paqueteEntity.getPeso());
        paqueteExistente.setVolumen(paqueteEntity.getVolumen());
        paqueteExistente.setDireccionDestino(paqueteEntity.getDireccionDestino());
        paqueteExistente.setIntentos(paqueteEntity.getIntentos());
        paqueteExistente.setFechaCompromiso(paqueteEntity.getFechaCompromiso());
        paqueteExistente.setDespachadoAt(paqueteEntity.getDespachadoAt());
        paqueteExistente.setEntregadoAt(paqueteEntity.getEntregadoAt());
        paqueteExistente.setUsuarioClienteId(paqueteEntity.getUsuarioClienteId());
        paqueteExistente.setCentroDistId(paqueteEntity.getCentroDistId());
        paqueteExistente.setPaqueteOrigenId(paqueteEntity.getPaqueteOrigenId());
        int filasAfectadas = paqueteRepository.update(paqueteExistente);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo actualizar el paquete");
        }
        return paqueteExistente;
    }


    public void deleteById(Integer id) {
        int filasAfectadas = paqueteRepository.deleteById(id);
        if(filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo eliminar el paquete");
        }
    }
}
