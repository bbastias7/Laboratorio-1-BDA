package com.example.demo.services;

import com.example.demo.entities.VehiculoEntity;
import com.example.demo.repositories.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class VehiculoService {
    private final VehiculoRepository vehiculoRepository;


    public List<VehiculoEntity> findAll() {
        return vehiculoRepository.findAll();
    }


    public VehiculoEntity findById(Integer id) {
        return vehiculoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Vehículo no encontrado con ID: " + id));
    }


    public VehiculoEntity findByPatente(String patente) {
        return vehiculoRepository.findByPatente(patente)
                .orElseThrow(() -> new NoSuchElementException("Vehículo no encontrado con patente: " + patente));
    }


    public List<VehiculoEntity> findByCapacidadPeso(Double peso) {
        return vehiculoRepository.findByCapacidadPeso(peso);
    }


    public List<VehiculoEntity> findByCapacidadVolumen(Double volumen) {
        return vehiculoRepository.findByCapacidadVolumen(volumen);
    }


    public VehiculoEntity saveVehiculo(VehiculoEntity vehiculo) {
        validarCapacidades(vehiculo);
        if (vehiculoRepository.findByPatente(vehiculo.getPatente()).isPresent()) {
            throw new IllegalArgumentException("La patente '" + vehiculo.getPatente() + "' ya está registrada.");
        }
        return vehiculoRepository.save(vehiculo);
    }


    public VehiculoEntity updateVehiculo(Integer id, VehiculoEntity vehiculoDetalles) {
        validarCapacidades(vehiculoDetalles);
        VehiculoEntity vehiculoExistente = findById(id);
        if (!vehiculoExistente.getPatente().equalsIgnoreCase(vehiculoDetalles.getPatente())) {
            vehiculoRepository.findByPatente(vehiculoDetalles.getPatente()).ifPresent(v -> {
                throw new IllegalArgumentException("La patente '" + vehiculoDetalles.getPatente() + "' ya pertenece a otro vehículo.");
            });
        }
        vehiculoExistente.setPatente(vehiculoDetalles.getPatente());
        vehiculoExistente.setCapacidadPeso(vehiculoDetalles.getCapacidadPeso());
        vehiculoExistente.setCapacidadVolumen(vehiculoDetalles.getCapacidadVolumen());
        vehiculoExistente.setCentroDistId(vehiculoDetalles.getCentroDistId());
        int filasAfectadas = vehiculoRepository.update(vehiculoExistente);
        if (filasAfectadas == 0) {
            throw new IllegalStateException("No se pudo actualizar el vehículo con ID: " + id);
        }
        return vehiculoExistente;
    }


    private void validarCapacidades(VehiculoEntity vehiculo) {
        if (vehiculo.getCapacidadPeso() == null || vehiculo.getCapacidadPeso() <= 0) {
            throw new IllegalArgumentException("La capacidad de peso debe ser mayor a 0.");
        }
        if (vehiculo.getCapacidadVolumen() == null || vehiculo.getCapacidadVolumen() <= 0) {
            throw new IllegalArgumentException("La capacidad de volumen debe ser mayor a 0.");
        }
    }
}