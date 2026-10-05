package co.italarm.api.ventas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La venta indica un cliente que no existe. */
public class ClienteNoExisteException extends NegocioException {

  public static final String CODIGO = "CLIENTE_NO_EXISTE";

  public ClienteNoExisteException() {
    super(TipoError.VALIDACION, CODIGO, "El cliente indicado no existe.");
  }
}
