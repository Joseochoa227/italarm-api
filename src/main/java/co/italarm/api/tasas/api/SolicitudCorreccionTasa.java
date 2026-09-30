package co.italarm.api.tasas.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Corrección de una tasa (RF-36), con doble digitación y motivo opcional. */
public record SolicitudCorreccionTasa(
    @NotNull(message = "Ingresa la tasa correcta.")
        @Digits(integer = 13, fraction = 6, message = "La tasa admite máximo 6 decimales.")
        BigDecimal valor,
    @NotNull(message = "Digita la tasa por segunda vez.")
        @Digits(integer = 13, fraction = 6, message = "La tasa admite máximo 6 decimales.")
        BigDecimal confirmacion,
    boolean aceptarVariacion,
    @Size(max = 300, message = "El motivo admite máximo 300 caracteres.") String motivo) {}
