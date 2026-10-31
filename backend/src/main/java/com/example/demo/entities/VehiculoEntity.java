package com.example.demo.entities;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoEntity {
    private Integer vehiculoId; //PK
    @NotNull @Positive
    private Integer centroDistId; //FK
    @NotBlank @Size(max=16)
    private String patente;
    @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2)
    private BigDecimal capacidadPeso;
    @NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2)
    private BigDecimal capacidadVolumen;
}
