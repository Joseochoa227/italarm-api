package co.italarm.api.usuarios.api;

import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.usuarios.aplicacion.ServicioContrasenas;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios")
public class UsuarioControlador {

  private final ServicioContrasenas contrasenas;

  public UsuarioControlador(ServicioContrasenas contrasenas) {
    this.contrasenas = contrasenas;
  }

  @PutMapping("/actual/contrasena")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(
      summary = "Cambiar la contraseña del usuario actual",
      description =
          "Conserva la sesión actual y cierra las demás sesiones del usuario. La nueva contraseña"
              + " debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un"
              + " signo.")
  @ApiResponse(responseCode = "204", description = "Contraseña cambiada")
  @ApiResponse(
      responseCode = "400",
      description = "VALIDACION, CONTRASENA_NO_COINCIDE o CONTRASENA_DEBIL",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "409",
      description = "CONTRASENA_ACTUAL_INCORRECTA",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  public void cambiarContrasena(
      @AuthenticationPrincipal UsuarioAutenticado usuario,
      @Valid @RequestBody SolicitudCambioContrasena solicitud) {
    contrasenas.cambiar(
        usuario,
        solicitud.contrasenaActual(),
        solicitud.contrasenaNueva(),
        solicitud.confirmacion());
  }
}
