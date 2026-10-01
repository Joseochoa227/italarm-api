package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Número de serie vacío o demasiado largo. */
public class SerialInvalidoException extends NegocioException {

  public static final String CODIGO = "SERIAL_INVALIDO";

  public SerialInvalidoException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
