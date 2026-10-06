package co.italarm.api.garantias.api;

import co.italarm.api.garantias.aplicacion.DatosReclamo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Reclamo de garantía: envía {@code instalacionId} o {@code serialId}, no ambos (RF-125).
 *
 * @param fecha vacía es hoy
 * @param solucion se puede escribir después (P-45)
 */
public record SolicitudReclamo(
    Long instalacionId,
    Long serialId,
    LocalDate fecha,
    @NotBlank(message = "Describe el problema.")
        @Size(max = 2000, message = "El problema admite máximo 2000 caracteres.")
        String problema,
    @Size(max = 2000, message = "La solución admite máximo 2000 caracteres.") String solucion) {

  DatosReclamo aDatos() {
    return new DatosReclamo(instalacionId, serialId, fecha, problema, solucion);
  }
}
