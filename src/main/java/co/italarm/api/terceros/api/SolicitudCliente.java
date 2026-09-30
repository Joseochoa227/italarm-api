package co.italarm.api.terceros.api;

import co.italarm.api.shared.api.Edicion;
import co.italarm.api.terceros.dominio.DatosCliente;
import co.italarm.api.terceros.dominio.TipoCliente;
import co.italarm.api.terceros.dominio.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos de un cliente. {@code version} solo se envía al editar. */
public record SolicitudCliente(
    @NotNull(message = "Elige el tipo de cliente.") TipoCliente tipo,
    @NotBlank(message = "Ingresa el nombre o razón social.")
        @Size(max = 150, message = "El nombre admite máximo 150 caracteres.")
        String nombre,
    TipoDocumento tipoDocumento,
    @Size(max = 30, message = "El documento admite máximo 30 caracteres.")
        @Pattern(
            regexp = "[0-9.\\s-]*",
            message = "El documento solo admite números, puntos y guion.")
        String numeroDocumento,
    @NotBlank(message = "Ingresa el teléfono o WhatsApp.")
        @Size(max = 25, message = "El teléfono es demasiado largo.")
        String telefono,
    @Email(message = "El correo no es válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        String correo,
    @Size(max = 200, message = "La dirección admite máximo 200 caracteres.") String direccion,
    @Size(max = 80, message = "La ciudad admite máximo 80 caracteres.") String ciudad,
    @NotNull(groups = Edicion.class, message = "Falta la versión del registro.") Long version) {

  DatosCliente aDatos() {
    return new DatosCliente(
        tipo, nombre, tipoDocumento, numeroDocumento, telefono, correo, direccion, ciudad);
  }
}
