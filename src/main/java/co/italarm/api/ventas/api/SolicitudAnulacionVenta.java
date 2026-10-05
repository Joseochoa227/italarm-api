package co.italarm.api.ventas.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo de la anulación (RF-73). */
public record SolicitudAnulacionVenta(
    @NotBlank(message = "Ingresa el motivo de la anulación.")
        @Size(max = 300, message = "El motivo admite máximo 300 caracteres.")
        String motivo) {}
