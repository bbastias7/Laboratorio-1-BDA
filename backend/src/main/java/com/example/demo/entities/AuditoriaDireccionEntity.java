package com.example.demo.entities;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaDireccionEntity {
    private Integer auditoriaId; //PK
    @NotNull @Positive
    private Integer paqueteId; //FK
    private Integer usuarioId; //FK
    @NotBlank @Size(max=255)
    private String direccionAnterior;
    @NotBlank @Size(max=255)
    private String direccionNueva;
    private LocalDateTime fecha;
}
