package co.italarm.api.documentos.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** El enlace de descarga fue adulterado o ya venció. */
public class EnlaceInvalidoException extends NegocioException {

  public static final String CODIGO = "ENLACE_INVALIDO";

  public EnlaceInvalidoException() {
    super(TipoError.ACCESO_DENEGADO, CODIGO, "El enlace no es válido o ya venció.");
  }
}
