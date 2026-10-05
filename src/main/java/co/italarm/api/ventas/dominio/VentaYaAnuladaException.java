package co.italarm.api.ventas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La venta ya estaba anulada. */
public class VentaYaAnuladaException extends NegocioException {

  public static final String CODIGO = "VENTA_YA_ANULADA";

  public VentaYaAnuladaException() {
    super(TipoError.CONFLICTO, CODIGO, "La venta ya estaba anulada.");
  }
}
