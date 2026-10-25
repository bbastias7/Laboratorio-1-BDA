package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CentroDistribucionEntity {
    private Integer id;
    private String nombre;
    private String direccion;
    private String ciudad;
}
