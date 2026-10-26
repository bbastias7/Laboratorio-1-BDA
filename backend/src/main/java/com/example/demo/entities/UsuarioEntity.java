package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity {
    private Integer usuarioId; //PK
    private String nombre;
    private String email;
    private String rol;
}
