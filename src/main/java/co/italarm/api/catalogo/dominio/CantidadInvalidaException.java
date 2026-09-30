package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La cantidad no respeta la unidad de medida (P-09). */
public class CantidadInvalidaException extends NegocioException {

  public static final String CODIGO = "CANTIDAD_INVALIDA";

  public CantidadInvalidaException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
