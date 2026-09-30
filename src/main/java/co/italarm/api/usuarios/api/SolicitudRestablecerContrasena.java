package co.italarm.api.usuarios.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Nueva contraseña para otro usuario, digitada dos veces. */
public record SolicitudRestablecerContrasena(
    @NotBlank(message = "Ingresa la nueva contraseña.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String contrasenaNueva,
    @NotBlank(message = "Confirma la nueva contraseña.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String confirmacion) {

  @Override
  public String toString() {
    return "SolicitudRestablecerContrasena[***]";
  }
}
