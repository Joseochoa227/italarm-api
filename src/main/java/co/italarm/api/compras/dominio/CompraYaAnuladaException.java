package co.italarm.api.compras.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La compra ya estaba anulada. */
public class CompraYaAnuladaException extends NegocioException {

  public static final String CODIGO = "COMPRA_YA_ANULADA";

  public CompraYaAnuladaException() {
    super(TipoError.CONFLICTO, CODIGO, "La compra ya estaba anulada.");
  }
}
