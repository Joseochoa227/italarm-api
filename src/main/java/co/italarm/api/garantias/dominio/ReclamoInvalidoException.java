package co.italarm.api.garantias.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Datos del reclamo que no cumplen las reglas (sin problema, sin objeto, fecha futura). */
public class ReclamoInvalidoException extends NegocioException {

  public static final String CODIGO = "RECLAMO_INVALIDO";

  public ReclamoInvalidoException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
