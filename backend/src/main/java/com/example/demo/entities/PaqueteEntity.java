package com.example.demo.entities;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaqueteEntity {
    private Integer paqueteId; //PK
    private Integer usuarioClienteId; //FK
    @NotNull @Positive
    private Integer centroDistId; //FK
    private Integer paqueteOrigenId; //FK
    @Size(max=64)
    private String codigo;
    private String estado;
    @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2)
    private BigDecimal peso;
    @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2)
    private BigDecimal volumen;
    @NotBlank @Size(max=255)
    private String direccionDestino;
    private Integer intentos;
    @NotNull
    private LocalDateTime fechaCompromiso;
    private LocalDateTime despachadoAt;
    private LocalDateTime entregadoAt;
    private LocalDateTime fechaReprogramada;
}
