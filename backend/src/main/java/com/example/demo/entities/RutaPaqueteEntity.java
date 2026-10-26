package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaPaqueteEntity {
    private Integer rutaId; //FK
    private Integer paqueteId; //FK
    private String resultado;
    private LocalDateTime fechaResultado;
}
