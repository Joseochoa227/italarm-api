package co.italarm.api.shared.dominio;

/**
 * Error de negocio con un código estable (por ejemplo {@code STOCK_INSUFICIENTE}) y un mensaje en
 * español para mostrar al usuario (RT-05, BP-16).
 */
public abstract class NegocioException extends RuntimeException {

  private final TipoError tipo;
  private final String codigo;

  protected NegocioException(TipoError tipo, String codigo, String mensaje) {
    super(mensaje);
    this.tipo = tipo;
    this.codigo = codigo;
  }

  public TipoError tipo() {
    return tipo;
  }

  public String codigo() {
    return codigo;
  }

  /** Lista de errores detallados que acompaña al error (por ejemplo, por fila), o null. */
  public Object detalles() {
    return null;
  }
}
