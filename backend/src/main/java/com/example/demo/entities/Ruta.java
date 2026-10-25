package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Ruta {
    private Integer id;
    private CentroDistribucion centro;
    private Usuario repartidor;
    private Vehiculo vehiculo;
    private LocalDateTime fecha;
}
