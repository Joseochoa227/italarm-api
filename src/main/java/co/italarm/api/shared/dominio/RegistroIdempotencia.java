package co.italarm.api.shared.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/** Una clave {@code Idempotency-Key} ya usada y el documento que creó (RT-07). */
@Entity
@Table(name = "idempotencia")
public class RegistroIdempotencia {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "usuario_id", nullable = false, updatable = false)
  private Long usuarioId;

  @Column(name = "operacion", nullable = false, updatable = false, length = 40)
  private String operacion;

  @Column(name = "clave", nullable = false, updatable = false, length = 100)
  private String clave;

  @Column(name = "documento_id")
  private Long documentoId;

  @Column(name = "creada_en", nullable = false, updatable = false)
  private Instant creadaEn;

  protected RegistroIdempotencia() {}

  public RegistroIdempotencia(Long usuarioId, String operacion, String clave, Instant creadaEn) {
    this.usuarioId = usuarioId;
    this.operacion = operacion;
    this.clave = clave;
    this.creadaEn = creadaEn;
  }

  public void asociar(Long documento) {
    this.documentoId = documento;
  }

  public Long getDocumentoId() {
    return documentoId;
  }
}
