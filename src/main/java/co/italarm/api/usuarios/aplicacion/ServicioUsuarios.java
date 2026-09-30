package co.italarm.api.usuarios.aplicacion;

import co.italarm.api.shared.dominio.RecursoNoEncontradoException;
import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.usuarios.dominio.NoPuedeDesactivarseException;
import co.italarm.api.usuarios.dominio.PoliticaContrasena;
import co.italarm.api.usuarios.dominio.UsarCambioDeContrasenaException;
import co.italarm.api.usuarios.dominio.Usuario;
import co.italarm.api.usuarios.dominio.UsuarioCorreoDuplicadoException;
import co.italarm.api.usuarios.infraestructura.SesionRepositorio;
import co.italarm.api.usuarios.infraestructura.UsuarioRepositorio;
import java.time.Clock;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestión de usuarios (RF-148, RU-04, P-15): listar, crear, desactivar, activar y restablecer la
 * contraseña. Todos tienen acceso completo (RU-02).
 */
@Service
public class ServicioUsuarios {

  private final UsuarioRepositorio usuarios;
  private final SesionRepositorio sesiones;
  private final PasswordEncoder codificador;
  private final Clock reloj;

  public ServicioUsuarios(
      UsuarioRepositorio usuarios,
      SesionRepositorio sesiones,
      PasswordEncoder codificador,
      Clock reloj) {
    this.usuarios = usuarios;
    this.sesiones = sesiones;
    this.codificador = codificador;
    this.reloj = reloj;
  }

  @Transactional(readOnly = true)
  public List<UsuarioVista> listar() {
    return usuarios.findAllByOrderByNombreAsc().stream().map(UsuarioVista::de).toList();
  }

  @Transactional
  public UsuarioVista crear(String nombre, String correo, String contrasena, String confirmacion) {
    PoliticaContrasena.exigir(contrasena, confirmacion);
    if (usuarios.existsByCorreo(Usuario.normalizarCorreo(correo))) {
      throw new UsuarioCorreoDuplicadoException();
    }
    Usuario usuario = Usuario.crear(nombre, correo, codificador.encode(contrasena));
    return UsuarioVista.de(usuarios.saveAndFlush(usuario));
  }

  /** Desactiva el usuario y cierra todas sus sesiones. Nadie se desactiva a sí mismo. */
  @Transactional
  public UsuarioVista desactivar(Long id, UsuarioAutenticado actual) {
    if (id.equals(actual.usuarioId())) {
      throw new NoPuedeDesactivarseException();
    }
    Usuario usuario = buscar(id);
    usuario.desactivar();
    sesiones.revocarTodas(id, reloj.instant());
    usuarios.flush();
    return UsuarioVista.de(usuario);
  }

  @Transactional
  public UsuarioVista activar(Long id) {
    Usuario usuario = buscar(id);
    usuario.activar();
    usuarios.flush();
    return UsuarioVista.de(usuario);
  }

  /**
   * Asigna una nueva contraseña a otro usuario (por ejemplo, si la olvidó) y cierra sus sesiones.
   * Para la propia se usa el cambio de contraseña, que pide la actual.
   */
  @Transactional
  public UsuarioVista restablecerContrasena(
      Long id, String nueva, String confirmacion, UsuarioAutenticado actual) {
    if (id.equals(actual.usuarioId())) {
      throw new UsarCambioDeContrasenaException();
    }
    PoliticaContrasena.exigir(nueva, confirmacion);
    Usuario usuario = buscar(id);
    usuario.asignarContrasena(codificador.encode(nueva));
    sesiones.revocarTodas(id, reloj.instant());
    usuarios.flush();
    return UsuarioVista.de(usuario);
  }

  private Usuario buscar(Long id) {
    return usuarios
        .findById(id)
        .orElseThrow(() -> new RecursoNoEncontradoException("El usuario no existe."));
  }
}
