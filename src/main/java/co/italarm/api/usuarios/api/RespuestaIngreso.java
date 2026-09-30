package co.italarm.api.usuarios.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** Token de sesión (el frontend lo guarda y lo envía en {@code Authorization: Bearer}). */
public record RespuestaIngreso(
    @Schema(description = "Token de sesión. Se entrega una sola vez.") String token,
    UsuarioActual usuario) {

  @Override
  public String toString() {
    return "RespuestaIngreso[usuario=" + usuario + "]";
  }
}
