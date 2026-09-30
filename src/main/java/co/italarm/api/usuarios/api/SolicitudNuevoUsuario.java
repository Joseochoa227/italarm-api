package co.italarm.api.usuarios.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos de un usuario nuevo. La contraseña inicial se digita dos veces. */
public record SolicitudNuevoUsuario(
    @NotBlank(message = "Ingresa el nombre.")
        @Size(max = 100, message = "El nombre admite máximo 100 caracteres.")
        String nombre,
    @NotBlank(message = "Ingresa el correo.")
        @Email(message = "El correo no es válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        String correo,
    @NotBlank(message = "Ingresa la contraseña inicial.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String contrasena,
    @NotBlank(message = "Confirma la contraseña.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String confirmacion) {

  @Override
  public String toString() {
    return "SolicitudNuevoUsuario[nombre=" + nombre + ", correo=" + correo + ", contrasena=***]";
  }
}
