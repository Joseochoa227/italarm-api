package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Un producto inactivo no se puede usar en documentos nuevos. */
public class ProductoInactivoException extends NegocioException {

  public static final String CODIGO = "PRODUCTO_INACTIVO";

  public ProductoInactivoException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
