package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Correo o contraseña incorrectos. El mensaje no revela cuál de los dos falló. */
public class CredencialesInvalidasException extends NegocioException {

  public static final String CODIGO = "CREDENCIALES_INVALIDAS";

  public CredencialesInvalidasException() {
    super(TipoError.NO_AUTENTICADO, CODIGO, "Correo o contraseña incorrectos.");
  }
}
