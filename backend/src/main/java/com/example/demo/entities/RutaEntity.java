package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaEntity {
    private Integer id;
    private CentroDistribucionEntity centro;
    private UsuarioEntity repartidor;
    private VehiculoEntity vehiculo;
    private LocalDateTime fecha;
}
