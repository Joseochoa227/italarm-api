package co.italarm.api.terceros.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Teléfono que no se puede usar en WhatsApp (P-10). */
public class TelefonoInvalidoException extends NegocioException {

  public static final String CODIGO = "TELEFONO_INVALIDO";

  public TelefonoInvalidoException() {
    super(
        TipoError.VALIDACION,
        CODIGO,
        "El teléfono no es válido. Escríbelo con indicativo internacional, por ejemplo +57 300 123 4567 o +58 412 123 4567.");
  }
}
