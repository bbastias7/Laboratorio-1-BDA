package com.example.demo.entities;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaEntity {
    private Integer rutaId; //PK
    @NotNull @Positive
    private Integer centroDistId; //FK
    @NotNull @Positive
    private Integer repartidorId; //FK
    @NotNull @Positive
    private Integer vehiculoId; //FK
    private LocalDateTime fecha;
}
