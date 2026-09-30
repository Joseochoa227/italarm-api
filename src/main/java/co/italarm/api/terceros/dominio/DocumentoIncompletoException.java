package co.italarm.api.terceros.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Se indicó el tipo de documento sin el número, o al revés. */
public class DocumentoIncompletoException extends NegocioException {

  public static final String CODIGO = "DOCUMENTO_INCOMPLETO";

  public DocumentoIncompletoException() {
    super(
        TipoError.VALIDACION,
        CODIGO,
        "Indica el tipo de documento (CC o NIT) y su número, o deja ambos vacíos.");
  }
}
