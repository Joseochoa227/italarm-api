package co.italarm.api.usuarios.api;

import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.usuarios.aplicacion.ConsultaUsuarios;
import co.italarm.api.usuarios.aplicacion.ServicioContrasenas;
import co.italarm.api.usuarios.aplicacion.ServicioUsuarios;
import co.italarm.api.usuarios.aplicacion.UsuarioReferencia;
import co.italarm.api.usuarios.aplicacion.UsuarioVista;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/usuarios")
@Tag(name = "Usuarios", description = "Cambio de contraseña y gestión de usuarios (RF-148)")
public class UsuarioControlador {

  private final ServicioContrasenas contrasenas;
  private final ServicioUsuarios usuarios;
  private final ConsultaUsuarios consulta;

  public UsuarioControlador(
      ServicioContrasenas contrasenas, ServicioUsuarios usuarios, ConsultaUsuarios consulta) {
    this.contrasenas = contrasenas;
    this.usuarios = usuarios;
    this.consulta = consulta;
  }

  @GetMapping
  @Operation(summary = "Todos los usuarios")
  public List<UsuarioVista> listar() {
    return usuarios.listar();
  }

  @GetMapping("/tecnicos")
  @Operation(summary = "Usuarios activos que se pueden elegir como técnicos (P-37)")
  public List<UsuarioReferencia> tecnicos() {
    return consulta.activos();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Crear un usuario con su contraseña inicial",
      description =
          "Errores: CONTRASENA_NO_COINCIDE, CONTRASENA_DEBIL (400); USUARIO_CORREO_DUPLICADO"
              + " (409).")
  public UsuarioVista crear(@Valid @RequestBody SolicitudNuevoUsuario solicitud) {
    return usuarios.crear(
        solicitud.nombre(), solicitud.correo(), solicitud.contrasena(), solicitud.confirmacion());
  }

  @PostMapping("/{id}/desactivar")
  @Operation(
      summary = "Desactivar un usuario y cerrar sus sesiones",
      description = "Error: NO_PUEDE_DESACTIVARSE_A_SI_MISMO (422).")
  public UsuarioVista desactivar(
      @PathVariable Long id, @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return usuarios.desactivar(id, usuario);
  }

  @PostMapping("/{id}/activar")
  @Operation(summary = "Activar un usuario")
  public UsuarioVista activar(@PathVariable Long id) {
    return usuarios.activar(id);
  }

  @PostMapping("/{id}/restablecer-contrasena")
  @Operation(
      summary = "Asignar una nueva contraseña a otro usuario y cerrar sus sesiones",
      description =
          "Errores: CONTRASENA_NO_COINCIDE, CONTRASENA_DEBIL (400); USAR_CAMBIO_DE_CONTRASENA"
              + " (422) si es el propio usuario.")
  public UsuarioVista restablecerContrasena(
      @PathVariable Long id,
      @Valid @RequestBody SolicitudRestablecerContrasena solicitud,
      @AuthenticationPrincipal UsuarioAutenticado usuario) {
    return usuarios.restablecerContrasena(
        id, solicitud.contrasenaNueva(), solicitud.confirmacion(), usuario);
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
