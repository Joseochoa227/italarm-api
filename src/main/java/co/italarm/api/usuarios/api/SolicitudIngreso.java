package co.italarm.api.usuarios.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Datos para iniciar sesión. */
public record SolicitudIngreso(
    @NotBlank(message = "Ingresa tu correo.")
        @Email(message = "El correo no es válido.")
        @Size(max = 254, message = "El correo es demasiado largo.")
        String correo,
    @NotBlank(message = "Ingresa tu contraseña.")
        @Size(max = 128, message = "La contraseña es demasiado larga.")
        String contrasena) {

  @Override
  public String toString() {
    return "SolicitudIngreso[correo=" + correo + ", contrasena=***]";
  }
}
