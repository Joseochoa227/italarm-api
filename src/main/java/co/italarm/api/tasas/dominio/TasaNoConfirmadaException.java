package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Las dos digitaciones de la tasa no coinciden (RF-35a, código de RT-05). */
public class TasaNoConfirmadaException extends NegocioException {

  public static final String CODIGO = "TASA_NO_CONFIRMADA";

  public TasaNoConfirmadaException() {
    super(TipoError.VALIDACION, CODIGO, "Las dos tasas digitadas no coinciden. Digítala de nuevo.");
  }
}
