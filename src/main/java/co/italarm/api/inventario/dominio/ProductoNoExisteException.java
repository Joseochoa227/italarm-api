package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Un documento menciona un producto que no existe. */
public class ProductoNoExisteException extends NegocioException {

  public static final String CODIGO = "PRODUCTO_NO_EXISTE";

  public ProductoNoExisteException(Long productoId) {
    super(TipoError.VALIDACION, CODIGO, "El producto " + productoId + " no existe.");
  }
}
