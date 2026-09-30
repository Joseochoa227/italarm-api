package co.italarm.api.usuarios.infraestructura;

import co.italarm.api.shared.api.RestriccionConocida;
import co.italarm.api.shared.api.RestriccionesModulo;
import co.italarm.api.shared.dominio.TipoError;
import co.italarm.api.usuarios.dominio.UsuarioCorreoDuplicadoException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Restricciones de la base de datos de usuarios y su error de negocio (BP-09). */
@Component
public class RestriccionesUsuarios implements RestriccionesModulo {

  @Override
  public List<RestriccionConocida> restricciones() {
    return List.of(
        new RestriccionConocida(
            "uq_usuario_correo",
            TipoError.CONFLICTO,
            UsuarioCorreoDuplicadoException.CODIGO,
            "Ya existe un usuario con ese correo."));
  }
}
