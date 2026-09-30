package co.italarm.api.terceros.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Ya existe un cliente con ese CC/NIT (P-12). */
public class ClienteDocumentoDuplicadoException extends NegocioException {

  public static final String CODIGO = "CLIENTE_DOCUMENTO_DUPLICADO";

  public ClienteDocumentoDuplicadoException() {
    super(TipoError.CONFLICTO, CODIGO, "Ya existe un cliente con ese documento.");
  }
}
