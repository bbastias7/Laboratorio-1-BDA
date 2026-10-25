package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Paquete {
    private Integer id;
    private String codigo;
    private String estado;
    private Double peso;
    private Double volumen;
    private String direccionDestino;
    private Integer intentos;
    private LocalDateTime fechaCompromiso;
    private LocalDateTime despachadoAt;
    private LocalDateTime entregadoAt;
    private Usuario cliente;
    private CentroDistribucion centro;
    private Paquete paqueteOrigen;
}
