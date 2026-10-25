package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehiculoEntity {
    private Integer id;
    private String patente;
    private Double capacidadPeso;
    private Double capacidadVolumen;
    private CentroDistribucionEntity centro;
}
