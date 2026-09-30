package co.italarm.api.usuarios.aplicacion;

import co.italarm.api.shared.seguridad.UsuarioAutenticado;
import co.italarm.api.shared.seguridad.ValidadorToken;
import co.italarm.api.usuarios.dominio.CredencialesInvalidasException;
import co.italarm.api.usuarios.dominio.Sesion;
import co.italarm.api.usuarios.dominio.TokenSesion;
import co.italarm.api.usuarios.dominio.Usuario;
import co.italarm.api.usuarios.infraestructura.SesionRepositorio;
import co.italarm.api.usuarios.infraestructura.UsuarioRepositorio;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ingreso, validación del token y cierre de sesión. */
@Service
public class ServicioSesion implements ValidadorToken {

  /** Longitud máxima aceptada para un token; los tokens válidos tienen 43 caracteres. */
  private static final int LONGITUD_MAXIMA_TOKEN = 128;

  private final UsuarioRepositorio usuarios;
  private final SesionRepositorio sesiones;
  private final PasswordEncoder codificador;
  private final Clock reloj;
  private final SecureRandom aleatorio = new SecureRandom();

  /**
   * Hash contra el que se compara cuando el correo no existe, para que la respuesta tarde lo mismo
   * y no revele qué correos están registrados.
   */
  private final String hashFicticio;

  public ServicioSesion(
      UsuarioRepositorio usuarios,
      SesionRepositorio sesiones,
      PasswordEncoder codificador,
      Clock reloj) {
    this.usuarios = usuarios;
    this.sesiones = sesiones;
    this.codificador = codificador;
    this.reloj = reloj;
    this.hashFicticio = codificador.encode("contrasena-ficticia-para-igualar-tiempos");
  }

  @Transactional
  public IngresoRealizado iniciarSesion(String correo, String contrasena, String agenteUsuario) {
    Optional<Usuario> encontrado = usuarios.findByCorreo(Usuario.normalizarCorreo(correo));
    String hash =
        encontrado
            .filter(Usuario::tieneContrasena)
            .map(Usuario::getContrasenaHash)
            .orElse(hashFicticio);
    boolean coincide = codificador.matches(contrasena, hash);
    Usuario usuario =
        encontrado
            .filter(u -> coincide && u.isActivo() && u.tieneContrasena())
            .orElseThrow(CredencialesInvalidasException::new);

    TokenSesion token = TokenSesion.generar(aleatorio);
    sesiones.save(Sesion.abrir(usuario.getId(), token.hash(), agenteUsuario, reloj.instant()));
    return new IngresoRealizado(token.valor(), datosDe(usuario));
  }

  @Override
  @Transactional
  public Optional<UsuarioAutenticado> validar(String token) {
    if (token == null || token.isBlank() || token.length() > LONGITUD_MAXIMA_TOKEN) {
      return Optional.empty();
    }
    return sesiones
        .findByTokenHashAndRevocadaEnIsNull(TokenSesion.hashDe(token))
        .flatMap(
            sesion ->
                usuarios
                    .findById(sesion.getUsuarioId())
                    .filter(Usuario::isActivo)
                    .map(
                        usuario -> {
                          sesion.registrarUso(reloj.instant());
                          return new UsuarioAutenticado(
                              usuario.getId(),
                              sesion.getId(),
                              usuario.getNombre(),
                              usuario.getCorreo());
                        }));
  }

  @Transactional
  public void cerrarSesion(Long sesionId) {
    sesiones.findById(sesionId).ifPresent(sesion -> sesion.revocar(reloj.instant()));
  }

  static DatosUsuario datosDe(Usuario usuario) {
    return new DatosUsuario(usuario.getId(), usuario.getNombre(), usuario.getCorreo());
  }
}
