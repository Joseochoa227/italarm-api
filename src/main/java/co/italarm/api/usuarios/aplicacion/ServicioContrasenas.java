package co.italarm.api.usuarios.aplicacion;

import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.usuarios.dominio.ContrasenaActualIncorrectaException;
import co.italarm.api.usuarios.dominio.ContrasenaDebilException;
import co.italarm.api.usuarios.dominio.ContrasenaNoCoincideException;
import co.italarm.api.usuarios.dominio.PoliticaContrasena;
import co.italarm.api.usuarios.dominio.Usuario;
import co.italarm.api.usuarios.infraestructura.SesionRepositorio;
import co.italarm.api.usuarios.infraestructura.UsuarioRepositorio;
import java.time.Clock;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Cambio de contraseña (RU-07) y asignación de la contraseña inicial (P-01). */
@Service
public class ServicioContrasenas {

  private final UsuarioRepositorio usuarios;
  private final SesionRepositorio sesiones;
  private final PasswordEncoder codificador;
  private final Clock reloj;

  public ServicioContrasenas(
      UsuarioRepositorio usuarios,
      SesionRepositorio sesiones,
      PasswordEncoder codificador,
      Clock reloj) {
    this.usuarios = usuarios;
    this.sesiones = sesiones;
    this.codificador = codificador;
    this.reloj = reloj;
  }

  /**
   * Cambia la contraseña del usuario de la sesión. Conserva la sesión actual y cierra las demás,
   * para que un dispositivo perdido deje de tener acceso.
   */
  @Transactional
  public void cambiar(
      UsuarioAutenticado actual, String contrasenaActual, String nueva, String confirmacion) {
    if (!nueva.equals(confirmacion)) {
      throw new ContrasenaNoCoincideException();
    }
    List<String> faltantes = PoliticaContrasena.incumplimientos(nueva);
    if (!faltantes.isEmpty()) {
      throw new ContrasenaDebilException(
          PoliticaContrasena.DESCRIPCION + " Falta: " + String.join(", ", faltantes) + ".");
    }
    Usuario usuario =
        usuarios
            .findById(actual.usuarioId())
            .orElseThrow(() -> new IllegalStateException("El usuario de la sesión no existe"));
    if (!codificador.matches(contrasenaActual, usuario.getContrasenaHash())) {
      throw new ContrasenaActualIncorrectaException();
    }
    if (codificador.matches(nueva, usuario.getContrasenaHash())) {
      throw new ContrasenaDebilException("La nueva contraseña debe ser distinta de la actual.");
    }
    usuario.asignarContrasena(codificador.encode(nueva));
    sesiones.revocarOtras(usuario.getId(), actual.sesionId(), reloj.instant());
  }

  /**
   * Asigna la contraseña inicial a los usuarios que aún no tienen una. Nunca sobrescribe una
   * contraseña existente. Devuelve cuántos usuarios la recibieron.
   */
  @Transactional
  public int asignarContrasenaInicial(String contrasenaInicial) {
    List<Usuario> sinContrasena = usuarios.findByContrasenaHashIsNullOrderById();
    sinContrasena.forEach(u -> u.asignarContrasena(codificador.encode(contrasenaInicial)));
    return sinContrasena.size();
  }

  @Transactional(readOnly = true)
  public long contarUsuariosSinContrasena() {
    return usuarios.findByContrasenaHashIsNullOrderById().size();
  }
}
