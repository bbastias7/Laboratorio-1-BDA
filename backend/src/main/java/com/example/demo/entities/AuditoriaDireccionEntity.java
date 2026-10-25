package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaDireccionEntity {
    private Integer id;
    private PaqueteEntity paquete;
    private String direccionAnterior;
    private String direccionNueva;
    private UsuarioEntity usuario;
    private LocalDateTime fecha;
}
