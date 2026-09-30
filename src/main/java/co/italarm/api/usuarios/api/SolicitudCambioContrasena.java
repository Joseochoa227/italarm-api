package co.italarm.api.usuarios.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para cambiar la contraseña (RU-07). La nueva se digita dos veces. */
public record SolicitudCambioContrasena(
    @NotBlank(message = "Ingresa tu contraseña actual.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String contrasenaActual,
    @NotBlank(message = "Ingresa la nueva contraseña.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String contrasenaNueva,
    @NotBlank(message = "Confirma la nueva contraseña.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String confirmacion) {

  @Override
  public String toString() {
    return "SolicitudCambioContrasena[***]";
  }
}
