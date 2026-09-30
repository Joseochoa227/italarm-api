package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Locale;

/** Usuario del sistema (RU-01). Ingresa con su correo (P-01). */
@Entity
@Table(name = "usuario")
public class Usuario extends EntidadMaestra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "nombre", nullable = false, length = 100)
  private String nombre;

  @Column(name = "correo", nullable = false, length = 254)
  private String correo;

  @Column(name = "contrasena_hash", length = 100)
  private String contrasenaHash;

  @Column(name = "activo", nullable = false)
  private boolean activo;

  protected Usuario() {}

  public static String normalizarCorreo(String correo) {
    return correo.trim().toLowerCase(Locale.ROOT);
  }

  public boolean tieneContrasena() {
    return contrasenaHash != null;
  }

  public void asignarContrasena(String nuevoHash) {
    this.contrasenaHash = nuevoHash;
  }

  public Long getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }

  public String getCorreo() {
    return correo;
  }

  public String getContrasenaHash() {
    return contrasenaHash;
  }

  public boolean isActivo() {
    return activo;
  }
}
