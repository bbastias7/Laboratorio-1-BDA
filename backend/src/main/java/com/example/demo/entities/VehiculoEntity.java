package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoEntity {
    private Integer vehiculoId; //PK
    private Integer centroDistId; //FK
    private String patente;
    private Double capacidadPeso;
    private Double capacidadVolumen;
}
