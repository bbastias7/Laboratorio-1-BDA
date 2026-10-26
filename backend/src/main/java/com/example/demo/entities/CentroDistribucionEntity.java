package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CentroDistribucionEntity {
    private Integer centroDistId; //PK
    private String nombre;
    private String direccion;
    private String ciudad;
}
