package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaEntity {
    private Integer rutaId; //PK
    private Integer centroDistId; //FK
    private Integer repartidorId; //FK
    private Integer vehiculoId; //FK
    private LocalDateTime fecha;
}
