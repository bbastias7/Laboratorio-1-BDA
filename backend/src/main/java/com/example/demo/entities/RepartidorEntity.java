package com.example.demo.entities;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepartidorEntity {
    private Integer repartidorId;
    private Integer usuarioId;
    private Integer centroDistId;
}
