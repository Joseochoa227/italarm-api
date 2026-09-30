package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La corrección tiene el mismo valor que la tasa actual. */
public class TasaSinCambioException extends NegocioException {

  public static final String CODIGO = "TASA_SIN_CAMBIO";

  public TasaSinCambioException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
