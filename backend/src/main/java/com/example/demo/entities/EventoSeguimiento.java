package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoSeguimiento {
    private Integer id;
    private Paquete paquete;
    private String estado;
    private String descripcion;
    private LocalDateTime fecha;
}
