package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Request/response shapes for the endpoints added on top of the entity CRUD. */
public final class Dtos {

    private Dtos() {}




    public record AsignarRutaRequest(@NotNull Integer centroDistId, @NotNull Integer repartidorId,
                                     @NotNull Integer vehiculoId, LocalDateTime fecha,
                                     @NotEmpty List<@NotNull @jakarta.validation.constraints.Positive Integer> paqueteIds) {}

    public record EntregaFallidaRequest(@NotNull Integer rutaId, String motivo) {}

    public record EntregadoRequest(@NotNull Integer rutaId) {}

    public record DireccionRequest(@NotBlank @Size(max=255) String direccionDestino) {}

    public record EventoView(String estado, String descripcion, LocalDateTime fecha) {}

    public record TrackingResponse(String codigo, String estado, String direccionDestino,
                                   LocalDateTime fechaCompromiso, LocalDateTime entregadoAt,
                                   Integer intentos, LocalDateTime fechaReprogramada, List<EventoView> eventos) {}

    public record EntregaView(Integer rutaId, LocalDateTime fecha, Integer paqueteId, String codigo,
                              String direccionDestino, String estado, String resultado, Integer intentos,
                              LocalDateTime fechaCompromiso) {}

    public record Rendimiento(Integer centroId, String centroNombre, Integer repartidorId, String repartidorNombre,
                              long entregasTotales, long entregasATiempo, long intentosTotales,
                              long paquetesAtendidos, BigDecimal pctATiempo, BigDecimal intentosPromedio) {}

    public record RendimientoCentro(Integer centroId, String centroNombre, long entregasTotales,
                                    long entregasATiempo, BigDecimal pctATiempo, BigDecimal intentosPromedio) {}

    public record Alertas(int umbral, List<Rendimiento> repartidores, List<RendimientoCentro> centros) {}
}
