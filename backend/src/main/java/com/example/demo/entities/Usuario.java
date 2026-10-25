package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private Integer id;
    private String nombre;
    private String email;
    private String password;
    private String rol;
}
