package co.italarm.api.instalaciones.api;

import co.italarm.api.instalaciones.dominio.Instalacion;
import co.italarm.api.shared.dominio.Moneda;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

/**
 * Datos descriptivos de una instalación que sí se pueden corregir (RF-122, P-44). La dirección y la
 * descripción vacías conservan las actuales.
 */
public record SolicitudEdicionInstalacion(
    @Size(max = 200, message = "La dirección admite máximo 200 caracteres.") String direccion,
    @Size(max = 2000, message = "La descripción admite máximo 2000 caracteres.") String descripcion,
    @NotEmpty(message = "Elige al menos un técnico.")
        @Size(max = 10, message = "Máximo 10 técnicos.")
        List<@NotNull(message = "Hay un técnico vacío.") Long> tecnicos,
    @Size(max = 2000, message = "Las condiciones admiten máximo 2000 caracteres.")
        String condicionesGarantia,
    @Size(max = 500, message = "Las observaciones admiten máximo 500 caracteres.")
        String observaciones,
    Set<Moneda> monedasComprobante,
    @NotNull(message = "Falta la versión del registro.") Long version) {

  Instalacion.Descripcion aDatos() {
    return new Instalacion.Descripcion(
        direccion, descripcion, tecnicos, condicionesGarantia, observaciones, monedasComprobante);
  }
}
