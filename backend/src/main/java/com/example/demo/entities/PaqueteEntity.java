package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaqueteEntity {
    private Integer paqueteId; //PK
    private Integer usuarioClienteId; //FK
    private Integer centroDistId; //FK
    private Integer paqueteOrigenId; //FK
    private String codigo;
    private String estado;
    private Double peso;
    private Double volumen;
    private String direccionDestino;
    private Integer intentos;
    private LocalDateTime fechaCompromiso;
    private LocalDateTime despachadoAt;
    private LocalDateTime entregadoAt;
}
