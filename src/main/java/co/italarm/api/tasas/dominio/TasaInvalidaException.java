package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** El valor de la tasa no es válido. */
public class TasaInvalidaException extends NegocioException {

  public static final String CODIGO = "TASA_INVALIDA";

  public TasaInvalidaException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
