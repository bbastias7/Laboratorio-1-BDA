package com.example.demo.entities;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEntity {
    private Integer usuarioId; //PK
    @NotBlank @Size(max=64)
    private String nombre;
    @NotBlank @Email @Size(max=128)
    private String email;
    @NotBlank @Pattern(regexp="cliente|repartidor|despachador")
    private String rol;
    @NotBlank @Size(max=128)
    private String keycloakId;
}
