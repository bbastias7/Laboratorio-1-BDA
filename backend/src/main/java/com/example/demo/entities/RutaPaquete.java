package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RutaPaquete {
    private Ruta ruta;
    private Paquete paquete;
    private String resultado;
    private LocalDateTime fechaResultado;
}
