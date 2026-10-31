package com.example.demo.entities;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CentroDistribucionEntity {
    private Integer centroDistId; //PK
    @NotBlank @Size(max=64)
    private String nombre;
    @NotBlank @Size(max=128)
    private String direccion;
    @NotBlank @Size(max=64)
    private String ciudad;
}
