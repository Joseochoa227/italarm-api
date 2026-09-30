package co.italarm.api.usuarios.api;

import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.usuarios.aplicacion.DatosUsuario;
import co.italarm.api.usuarios.aplicacion.IngresoRealizado;
import co.italarm.api.usuarios.aplicacion.ServicioSesion;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sesion")
@Tag(name = "Sesión", description = "Ingreso, usuario actual y cierre de sesión")
public class SesionControlador {

  private final ServicioSesion servicio;

  public SesionControlador(ServicioSesion servicio) {
    this.servicio = servicio;
  }

  @PostMapping
  @SecurityRequirements
  @Operation(summary = "Iniciar sesión con correo y contraseña")
  @ApiResponse(responseCode = "200", description = "Sesión iniciada")
  @ApiResponse(
      responseCode = "400",
      description = "VALIDACION",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  @ApiResponse(
      responseCode = "401",
      description = "CREDENCIALES_INVALIDAS",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  public RespuestaIngreso iniciar(
      @Valid @RequestBody SolicitudIngreso solicitud,
      @RequestHeader(value = HttpHeaders.USER_AGENT, required = false) String agenteUsuario) {
    IngresoRealizado ingreso =
        servicio.iniciarSesion(solicitud.correo(), solicitud.contrasena(), agenteUsuario);
    return new RespuestaIngreso(ingreso.token(), aRespuesta(ingreso.usuario()));
  }

  @GetMapping
  @Operation(summary = "Usuario de la sesión actual")
  @ApiResponse(responseCode = "200", description = "Sesión activa")
  @ApiResponse(
      responseCode = "401",
      description = "NO_AUTENTICADO",
      content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
  public UsuarioActual actual(@AuthenticationPrincipal UsuarioAutenticado usuario) {
    return new UsuarioActual(usuario.usuarioId(), usuario.nombre(), usuario.correo());
  }

  @DeleteMapping
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Cerrar la sesión actual")
  @ApiResponse(responseCode = "204", description = "Sesión cerrada")
  public void cerrar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
    servicio.cerrarSesion(usuario.sesionId());
  }

  private static UsuarioActual aRespuesta(DatosUsuario datos) {
    return new UsuarioActual(datos.id(), datos.nombre(), datos.correo());
  }
}
