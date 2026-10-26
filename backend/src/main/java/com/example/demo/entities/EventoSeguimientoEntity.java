package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoSeguimientoEntity {
    private Integer eventoId; //PK
    private Integer paqueteId; //FK
    private String estado;
    private String descripcion;
    private LocalDateTime fecha;
}
