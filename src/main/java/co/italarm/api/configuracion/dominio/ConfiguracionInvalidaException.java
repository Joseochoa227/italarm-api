package co.italarm.api.configuracion.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Un valor de la configuración está fuera de lo permitido. */
public class ConfiguracionInvalidaException extends NegocioException {

  public static final String CODIGO = "CONFIGURACION_INVALIDA";

  public ConfiguracionInvalidaException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
