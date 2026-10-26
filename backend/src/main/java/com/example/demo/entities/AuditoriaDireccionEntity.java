package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaDireccionEntity {
    private Integer auditoriaId; //PK
    private Integer paqueteId; //FK
    private Integer usuarioId; //FK
    private String direccionAnterior;
    private String direccionNueva;
    private LocalDateTime fecha;
}
