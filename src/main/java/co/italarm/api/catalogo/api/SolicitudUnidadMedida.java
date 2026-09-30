package co.italarm.api.catalogo.api;

import co.italarm.api.shared.api.Edicion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de una unidad de medida. {@code version} solo se envía al editar. */
public record SolicitudUnidadMedida(
    @NotBlank(message = "Ingresa el nombre de la unidad.")
        @Size(max = 40, message = "El nombre admite máximo 40 caracteres.")
        String nombre,
    @NotBlank(message = "Ingresa la abreviatura.")
        @Size(max = 10, message = "La abreviatura admite máximo 10 caracteres.")
        String abreviatura,
    @NotNull(message = "Indica si la unidad admite decimales.") Boolean admiteDecimales,
    @NotNull(groups = Edicion.class, message = "Falta la versión del registro.") Long version) {}
