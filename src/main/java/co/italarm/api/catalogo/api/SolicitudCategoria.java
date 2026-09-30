package co.italarm.api.catalogo.api;

import co.italarm.api.shared.api.Edicion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos de una categoría. {@code version} solo se envía al editar. */
public record SolicitudCategoria(
    @NotBlank(message = "Ingresa el nombre de la categoría.")
        @Size(max = 80, message = "El nombre admite máximo 80 caracteres.")
        String nombre,
    @NotNull(groups = Edicion.class, message = "Falta la versión del registro.") Long version) {}
