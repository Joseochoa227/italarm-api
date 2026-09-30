package co.italarm.api.usuarios.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;

/**
 * Sesión abierta con un token. No vence (P-03): termina al cerrar sesión o al cambiar la
 * contraseña.
 */
@Entity
@Table(name = "sesion")
public class Sesion {

  /** Cada cuánto se actualiza el último uso, para no escribir en cada petición. */
  static final Duration INTERVALO_REGISTRO_USO = Duration.ofMinutes(5);

  private static final int LONGITUD_AGENTE = 300;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false, updatable = false)
  private Long usuarioId;

  @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
  private String tokenHash;

  @Column(name = "agente_usuario", length = LONGITUD_AGENTE, updatable = false)
  private String agenteUsuario;

  @Column(name = "creada_en", nullable = false, updatable = false)
  private Instant creadaEn;

  @Column(name = "ultimo_uso", nullable = false)
  private Instant ultimoUso;

  @Column(name = "revocada_en")
  private Instant revocadaEn;

  protected Sesion() {}

  public static Sesion abrir(
      Long usuarioId, String tokenHash, String agenteUsuario, Instant ahora) {
    Sesion sesion = new Sesion();
    sesion.usuarioId = usuarioId;
    sesion.tokenHash = tokenHash;
    sesion.agenteUsuario =
        agenteUsuario == null || agenteUsuario.length() <= LONGITUD_AGENTE
            ? agenteUsuario
            : agenteUsuario.substring(0, LONGITUD_AGENTE);
    sesion.creadaEn = ahora;
    sesion.ultimoUso = ahora;
    return sesion;
  }

  public boolean estaActiva() {
    return revocadaEn == null;
  }

  public void revocar(Instant ahora) {
    if (estaActiva()) {
      revocadaEn = ahora;
    }
  }

  /** Registra el uso si pasó el intervalo mínimo; devuelve si hubo cambio. */
  public boolean registrarUso(Instant ahora) {
    if (Duration.between(ultimoUso, ahora).compareTo(INTERVALO_REGISTRO_USO) >= 0) {
      ultimoUso = ahora;
      return true;
    }
    return false;
  }

  public Long getId() {
    return id;
  }

  public Long getUsuarioId() {
    return usuarioId;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public String getAgenteUsuario() {
    return agenteUsuario;
  }

  public Instant getCreadaEn() {
    return creadaEn;
  }

  public Instant getUltimoUso() {
    return ultimoUso;
  }

  public Instant getRevocadaEn() {
    return revocadaEn;
  }
}
