package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La tasa de hoy ya existe; se corrige en lugar de registrarla de nuevo. */
public class TasaYaRegistradaException extends NegocioException {

  public static final String CODIGO = "TASA_YA_REGISTRADA";

  public TasaYaRegistradaException(String mensaje) {
    super(TipoError.CONFLICTO, CODIGO, mensaje);
  }
}
