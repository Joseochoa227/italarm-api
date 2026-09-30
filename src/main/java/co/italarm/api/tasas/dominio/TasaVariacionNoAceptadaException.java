package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La variación supera el límite y no se aceptó expresamente (RF-35c). */
public class TasaVariacionNoAceptadaException extends NegocioException {

  public static final String CODIGO = "TASA_VARIACION_NO_ACEPTADA";

  public TasaVariacionNoAceptadaException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
