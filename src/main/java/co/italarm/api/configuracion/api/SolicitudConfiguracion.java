package co.italarm.api.configuracion.api;

import co.italarm.api.configuracion.dominio.DatosConfiguracion;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Datos de la empresa y valores por defecto (RF-145 a RF-147). */
public record SolicitudConfiguracion(
    @NotBlank(message = "Ingresa el nombre de la empresa.")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
        String empresaNombre,
    @Size(max = 150, message = "El lema admite máximo 150 caracteres.") String empresaLema,
    @Size(max = 30, message = "El NIT admite máximo 30 caracteres.") String empresaNit,
    @Size(max = 80, message = "La ciudad admite máximo 80 caracteres.") String empresaCiudad,
    @Size(max = 30, message = "El teléfono admite máximo 30 caracteres.") String empresaTelefono,
    @Email(message = "El correo no es válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        String empresaCorreo,
    @NotNull(message = "Ingresa el límite de variación de tasas.")
        @Digits(integer = 3, fraction = 2, message = "El límite admite máximo 2 decimales.")
        BigDecimal limiteVariacionTasa,
    @NotNull(message = "Elige la validez de las cotizaciones.") Integer validezCotizacionDias,
    @NotNull(message = "Ingresa la garantía de mano de obra.") Integer garantiaManoObraMeses,
    @NotNull(message = "Ingresa la garantía de equipos.") Integer garantiaEquiposMeses,
    @NotBlank(message = "Ingresa las condiciones de garantía.")
        @Size(max = 2000, message = "Las condiciones admiten máximo 2000 caracteres.")
        String condicionesGarantia,
    @NotBlank(message = "Ingresa el pie de los PDF.")
        @Size(max = 2000, message = "El pie admite máximo 2000 caracteres.")
        String piePdf,
    @NotNull(message = "Falta la versión del registro.") Long version) {

  DatosConfiguracion aDatos() {
    return new DatosConfiguracion(
        empresaNombre,
        empresaLema,
        empresaNit,
        empresaCiudad,
        empresaTelefono,
        empresaCorreo,
        limiteVariacionTasa,
        validezCotizacionDias,
        garantiaManoObraMeses,
        garantiaEquiposMeses,
        condicionesGarantia,
        piePdf);
  }
}
