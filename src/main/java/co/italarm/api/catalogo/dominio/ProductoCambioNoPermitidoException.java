package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Cambio de serial o unidad en un producto con movimientos (P-17). */
public class ProductoCambioNoPermitidoException extends NegocioException {

  public static final String CODIGO = "PRODUCTO_CAMBIO_NO_PERMITIDO";

  public ProductoCambioNoPermitidoException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
