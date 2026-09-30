package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Para la propia contraseña se usa el cambio de contraseña, que pide la actual. */
public class UsarCambioDeContrasenaException extends NegocioException {

  public static final String CODIGO = "USAR_CAMBIO_DE_CONTRASENA";

  public UsarCambioDeContrasenaException() {
    super(
        TipoError.REGLA_NEGOCIO,
        CODIGO,
        "Para cambiar tu propia contraseña usa la opción Cambiar contraseña.");
  }
}
