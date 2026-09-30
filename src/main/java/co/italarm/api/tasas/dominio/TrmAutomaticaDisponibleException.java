package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Ya está la TRM oficial de hoy; la manual solo aplica si falló (RF-33). */
public class TrmAutomaticaDisponibleException extends NegocioException {

  public static final String CODIGO = "TRM_AUTOMATICA_DISPONIBLE";

  public TrmAutomaticaDisponibleException(String mensaje) {
    super(TipoError.CONFLICTO, CODIGO, mensaje);
  }
}
