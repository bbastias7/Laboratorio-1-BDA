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

    /** Public self-registration: the role is always cliente, so it is deliberately not part of the request. */
    public record RegistroRequest(
            @NotBlank(message = "es obligatorio") @Size(max = 64, message = "máximo 64 caracteres") String nombre,
            @NotBlank(message = "es obligatorio") @jakarta.validation.constraints.Email(message = "no es un correo válido")
            @Size(max = 128, message = "máximo 128 caracteres") String email,
            @NotBlank(message = "es obligatoria")
            @Size(min = 8, max = 128, message = "debe tener entre 8 y 128 caracteres") String password) {}

    /**
     * Staff creation by a despachador. Exactly one of {@code password} (the backend creates the Keycloak account with a
     * temporary password) or {@code keycloakId} (links an existing account) must be present; the service enforces it.
     */
    public record UsuarioCrearRequest(
            @NotBlank(message = "es obligatorio") @Size(max = 64, message = "máximo 64 caracteres") String nombre,
            @NotBlank(message = "es obligatorio") @jakarta.validation.constraints.Email(message = "no es un correo válido")
            @Size(max = 128, message = "máximo 128 caracteres") String email,
            @NotBlank(message = "es obligatorio")
            @jakarta.validation.constraints.Pattern(regexp = "cliente|repartidor|despachador",
                    message = "debe ser cliente, repartidor o despachador") String rol,
            @Size(max = 128, message = "debe tener como máximo 128 caracteres") String password,
            @Size(max = 128, message = "máximo 128 caracteres") String keycloakId) {}

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
