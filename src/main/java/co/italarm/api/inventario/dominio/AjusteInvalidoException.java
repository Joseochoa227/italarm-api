package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Datos del ajuste incompletos o inválidos. */
public class AjusteInvalidoException extends NegocioException {

  public static final String CODIGO = "AJUSTE_INVALIDO";

  public AjusteInvalidoException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
