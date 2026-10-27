package com.example.demo.services;

import com.example.demo.entities.RutaEntity;
import com.example.demo.repositories.RutaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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


    public RutaEntity findById(Integer id) {
        return rutaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Ruta no encontrada con ID: " + id));
    }

    /*
    public List<RutaEntity> findByCentroId(Integer centro_id) {
        return rutaRepository.find
    }


    public List<RutaEntity> findByRepartidorId(Integer repartidor_id) {

    }


    public List<RutaEntity> findByVehiculoId(Integer vehiculo_id) {

    }


    public RutaEntity findByFecha(LocalDateTime fecha) {

    }
    */

    public RutaEntity saveRuta(RutaEntity ruta) {
        validarCamposClave(ruta);
        if (ruta.getFecha() == null) {
            ruta.setFecha(LocalDateTime.now());
        }
        return rutaRepository.save(ruta);
    }


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