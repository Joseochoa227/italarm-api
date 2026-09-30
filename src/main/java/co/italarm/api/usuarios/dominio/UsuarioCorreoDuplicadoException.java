package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Ya existe un usuario con ese correo. */
public class UsuarioCorreoDuplicadoException extends NegocioException {

  public static final String CODIGO = "USUARIO_CORREO_DUPLICADO";

  public UsuarioCorreoDuplicadoException() {
    super(TipoError.CONFLICTO, CODIGO, "Ya existe un usuario con ese correo.");
  }
}
