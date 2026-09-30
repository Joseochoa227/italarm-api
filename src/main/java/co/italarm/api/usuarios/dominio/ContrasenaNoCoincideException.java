package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La nueva contraseña y su confirmación son distintas. */
public class ContrasenaNoCoincideException extends NegocioException {

  public static final String CODIGO = "CONTRASENA_NO_COINCIDE";

  public ContrasenaNoCoincideException() {
    super(TipoError.VALIDACION, CODIGO, "La nueva contraseña y su confirmación no coinciden.");
  }
}
