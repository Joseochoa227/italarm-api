package co.italarm.api.shared.seguridad;

import java.util.Optional;

/** Valida el token de sesión recibido en la cabecera {@code Authorization: Bearer}. */
public interface ValidadorToken {

  /** Devuelve el usuario si el token corresponde a una sesión activa de un usuario activo. */
  Optional<UsuarioAutenticado> validar(String token);
}
