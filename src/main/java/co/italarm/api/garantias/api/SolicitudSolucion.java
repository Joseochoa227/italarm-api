package co.italarm.api.garantias.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Solución de un reclamo (P-45). */
public record SolicitudSolucion(
    @Size(max = 2000, message = "La solución admite máximo 2000 caracteres.") String solucion,
    @NotNull(message = "Falta la versión del registro.") Long version) {}
