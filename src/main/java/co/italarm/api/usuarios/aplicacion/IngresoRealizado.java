package co.italarm.api.usuarios.aplicacion;

/** Resultado de un ingreso: el token (se entrega una sola vez) y el usuario. */
public record IngresoRealizado(String token, DatosUsuario usuario) {

  @Override
  public String toString() {
    return "IngresoRealizado[usuario=" + usuario + "]";
  }
}
