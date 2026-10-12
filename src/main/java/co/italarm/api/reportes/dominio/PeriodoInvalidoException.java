package co.italarm.api.reportes.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** El rango de fechas del reporte no es válido. */
public class PeriodoInvalidoException extends NegocioException {

  public static final String CODIGO = "PERIODO_INVALIDO";

  public PeriodoInvalidoException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
