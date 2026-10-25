package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repartidor {
    private Integer id;
    private Usuario usuario;
    private CentroDistribucion centro;
}
