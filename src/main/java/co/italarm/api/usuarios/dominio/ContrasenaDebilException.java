package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La nueva contraseña no cumple la política o es igual a la actual. */
public class ContrasenaDebilException extends NegocioException {

  public static final String CODIGO = "CONTRASENA_DEBIL";

  public ContrasenaDebilException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
