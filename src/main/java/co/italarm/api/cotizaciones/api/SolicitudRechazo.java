package co.italarm.api.cotizaciones.api;

import co.italarm.api.cotizaciones.dominio.MotivoRechazo;
import jakarta.validation.constraints.Size;

/**
 * Rechazo de una cotización; ambos datos son opcionales (P-51).
 *
 * @param motivo PRECIO, COMPETENCIA u OTRO
 */
public record SolicitudRechazo(
    MotivoRechazo motivo,
    @Size(max = 300, message = "El detalle admite máximo 300 caracteres.") String detalle) {}
