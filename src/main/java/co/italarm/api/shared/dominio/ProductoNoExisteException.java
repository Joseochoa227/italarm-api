package co.italarm.api.shared.dominio;

/** Un documento menciona un producto que no existe. */
public class ProductoNoExisteException extends NegocioException {

  public static final String CODIGO = "PRODUCTO_NO_EXISTE";

  public ProductoNoExisteException(Long productoId) {
    super(TipoError.VALIDACION, CODIGO, "El producto " + productoId + " no existe.");
  }
}
