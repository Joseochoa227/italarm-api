package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Ya existe un producto con ese código. */
public class ProductoCodigoDuplicadoException extends NegocioException {

  public static final String CODIGO = "PRODUCTO_CODIGO_DUPLICADO";

  public ProductoCodigoDuplicadoException(String mensaje) {
    super(TipoError.CONFLICTO, CODIGO, mensaje);
  }
}
