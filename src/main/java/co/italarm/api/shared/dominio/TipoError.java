package co.italarm.api.shared.dominio;

/** Clase de error de negocio; la capa api la traduce al estado HTTP. */
public enum TipoError {
  VALIDACION("Datos inválidos"),
  NO_AUTENTICADO("No autenticado"),
  ACCESO_DENEGADO("Acceso denegado"),
  NO_ENCONTRADO("No encontrado"),
  CONFLICTO("Conflicto"),
  REGLA_NEGOCIO("Regla de negocio incumplida"),
  INTERNO("Error interno");

  private final String titulo;

  TipoError(String titulo) {
    this.titulo = titulo;
  }

  public String titulo() {
    return titulo;
  }
}
