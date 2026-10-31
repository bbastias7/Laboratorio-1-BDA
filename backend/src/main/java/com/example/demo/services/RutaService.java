package com.example.demo.services;

import com.example.demo.entities.RutaEntity;
import com.example.demo.repositories.RutaRepository;
import lombok.RequiredArgsConstructor;
import com.example.demo.config.AuthUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RutaService {
    private final RutaRepository rutaRepository;


    public List<RutaEntity> findAll() {
        return rutaRepository.findAll();
    }


    /** despachador sees all routes; repartidor only its own. */
    public List<RutaEntity> findVisibles(AuthUser u) {
        return u.esDespachador() ? rutaRepository.findAll() : rutaRepository.findByUsuarioRepartidor(u.uid());
    }

    public RutaEntity findVisibleById(Integer id, AuthUser u) {
        RutaEntity r = findById(id);
        if (!u.esDespachador() && !rutaRepository.esDeUsuario(id, u.uid())) {
            throw new AccessDeniedException("Ruta de otro repartidor");
        }
        return r;
    }

    /** Atomic assignment through the asignar_ruta stored procedure (capacity and state rules live in the DB). */
    @Transactional
    public RutaEntity asignar(Integer centroId, Integer repartidorId, Integer vehiculoId,
                              LocalDateTime fecha, List<Integer> paqueteIds) {
        int id = rutaRepository.asignarRuta(centroId, repartidorId, vehiculoId,
                fecha == null ? LocalDateTime.now() : fecha, paqueteIds);
        return findById(id);
    }

    public RutaEntity findById(Integer id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ruta no encontrada con ID: " + id));
    }


    @Transactional
    public RutaEntity saveRuta(RutaEntity ruta) {
        validarCamposClave(ruta);
        if (ruta.getFecha() == null) {
            ruta.setFecha(LocalDateTime.now());
        }
        return rutaRepository.save(ruta);
    }


    @Transactional
    public RutaEntity updateRuta(Integer id, RutaEntity rutaDetalles) {
        validarCamposClave(rutaDetalles);
        RutaEntity rutaExistente = findById(id);
        rutaExistente.setCentroDistId(rutaDetalles.getCentroDistId());
        rutaExistente.setRepartidorId(rutaDetalles.getRepartidorId());
        rutaExistente.setVehiculoId(rutaDetalles.getVehiculoId());
        if (rutaDetalles.getFecha() != null) {
            rutaExistente.setFecha(rutaDetalles.getFecha());
        }
        int filasAfectadas = rutaRepository.update(rutaExistente);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo actualizar la ruta con ID: " + id);
        }
        return rutaExistente;
    }


    @Transactional
    public void deleteById(Integer id) {
        int filasAfectadas = rutaRepository.deleteById(id);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo eliminar la ruta con ID: " + id);
        }
    }


    private void validarCamposClave(RutaEntity ruta) {
        if (ruta.getCentroDistId() == null) {
            throw new IllegalArgumentException("El ID del centro de distribución es obligatorio");
        }
        if (ruta.getRepartidorId() == null) {
            throw new IllegalArgumentException("El ID del repartidor es obligatorio");
        }
        if (ruta.getVehiculoId() == null) {
            throw new IllegalArgumentException("El ID del vehículo es obligatorio");
        }
    }
}
