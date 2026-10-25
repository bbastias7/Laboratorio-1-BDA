package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaDireccion {
    private Integer id;
    private Paquete paquete;
    private String direccionAnterior;
    private String direccionNueva;
    private Usuario usuario;
    private LocalDateTime fecha;
}
