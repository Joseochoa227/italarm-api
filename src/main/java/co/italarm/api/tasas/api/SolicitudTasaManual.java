package co.italarm.api.tasas.api;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Registro manual de la tasa de hoy (RF-35): el valor se digita dos veces y, si la variación supera
 * el límite, se debe enviar {@code aceptarVariacion: true}.
 */
public record SolicitudTasaManual(
    @NotNull(message = "Ingresa la tasa.")
        @Digits(integer = 13, fraction = 6, message = "La tasa admite máximo 6 decimales.")
        BigDecimal valor,
    @NotNull(message = "Digita la tasa por segunda vez.")
        @Digits(integer = 13, fraction = 6, message = "La tasa admite máximo 6 decimales.")
        BigDecimal confirmacion,
    boolean aceptarVariacion) {}
