package com.example.demo.services;
import com.example.demo.dto.Dtos.EntregaView;
import com.example.demo.config.AuthUser;
import com.example.demo.repositories.RutaPaqueteRepository;

import com.example.demo.entities.RepartidorEntity;
import com.example.demo.repositories.RepartidorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RepartidorService {
    private final RepartidorRepository repartidorRepository;
    private final RutaPaqueteRepository rutaPaqueteRepository;


    public List<RepartidorEntity> findAll() {
        return repartidorRepository.findAll();
    }


    public RepartidorEntity findById(int id) {
        return repartidorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Repartidor inexistente"));
    }


    @Transactional
    public RepartidorEntity saveRepartidor(RepartidorEntity repartidor) {
        if (repartidor.getRepartidorId() != null && repartidorRepository.findById(repartidor.getRepartidorId()).isPresent()) {
            throw new IllegalArgumentException("El repartidor ya existe en la plataforma");
        }
        return repartidorRepository.save(repartidor);
    }


    @Transactional
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


    @Transactional
    public void deleteById(int id) {
        findById(id);
        int filasAfectadas = repartidorRepository.deleteById(id);
        if(filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo eliminar el repartidor");
        }
    }
    public List<EntregaView> listarEntregas(
            AuthUser usuario, boolean soloPendientes) {
        return rutaPaqueteRepository.listarDeUsuario(usuario.uid(), soloPendientes);
    }
}
