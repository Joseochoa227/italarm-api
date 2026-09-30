package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Un producto con movimientos no se elimina (RF-14). */
public class ProductoConMovimientosException extends NegocioException {

  public static final String CODIGO = "PRODUCTO_CON_MOVIMIENTOS";

  public ProductoConMovimientosException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
