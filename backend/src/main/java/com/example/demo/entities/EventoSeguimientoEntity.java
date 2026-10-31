package com.example.demo.entities;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventoSeguimientoEntity {
    private Integer eventoId; //PK
    @NotNull @Positive
    private Integer paqueteId; //FK
    @NotBlank @Pattern(regexp="Registrado|En centro|En reparto|Entregado|Reprogramado|Devuelto a origen")
    private String estado;
    @NotBlank @Size(max=255)
    private String descripcion;
    private LocalDateTime fecha;
}
