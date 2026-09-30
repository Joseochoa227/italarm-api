package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Al cambiar la contraseña, la actual no coincide. */
public class ContrasenaActualIncorrectaException extends NegocioException {

  public static final String CODIGO = "CONTRASENA_ACTUAL_INCORRECTA";

  public ContrasenaActualIncorrectaException() {
    super(TipoError.CONFLICTO, CODIGO, "La contraseña actual no es correcta.");
  }
}
