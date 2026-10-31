package com.example.demo.entities;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepartidorEntity {
    private Integer repartidorId;
    @NotNull @Positive
    private Integer usuarioId;
    @NotNull @Positive
    private Integer centroDistId;
}
