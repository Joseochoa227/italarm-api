package co.italarm.api.usuarios.aplicacion;

import co.italarm.api.usuarios.dominio.Usuario;
import co.italarm.api.usuarios.infraestructura.UsuarioRepositorio;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Nombres de usuarios para mostrar quién registró cada movimiento (RU-03). */
@Service
public class ConsultaUsuarios {

  private final UsuarioRepositorio usuarios;

  public ConsultaUsuarios(UsuarioRepositorio usuarios) {
    this.usuarios = usuarios;
  }

  @Transactional(readOnly = true)
  public Map<Long, String> nombres(Collection<Long> ids) {
    return usuarios.findAllById(ids).stream()
        .collect(Collectors.toMap(Usuario::getId, Usuario::getNombre));
  }
}
