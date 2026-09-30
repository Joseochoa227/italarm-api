package co.italarm.api.tasas.api;

import co.italarm.api.tasas.dominio.ParMoneda;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Vista previa de una tasa antes de guardarla (RF-35b). Con {@code tasaId} compara con esa tasa
 * (corrección); sin él, con la última anterior a hoy (registro nuevo).
 */
public record SolicitudVistaPrevia(
    @NotNull(message = "Indica el par de monedas.") ParMoneda par,
    @NotNull(message = "Ingresa la tasa.")
        @DecimalMin(value = "0", inclusive = false, message = "La tasa debe ser mayor que 0.")
        @Digits(integer = 13, fraction = 6, message = "La tasa admite máximo 6 decimales.")
        BigDecimal valor,
    Long tasaId) {}
