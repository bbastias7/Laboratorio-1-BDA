package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepartidorEntity {
    private Integer id;
    private UsuarioEntity usuario;
    private CentroDistribucionEntity centro;
}
