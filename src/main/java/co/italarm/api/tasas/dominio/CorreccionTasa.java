package co.italarm.api.tasas.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

/** Corrección de una tasa (RF-36). Solo se inserta; nunca se modifica. */
@Entity
@Table(name = "correccion_tasa")
public class CorreccionTasa {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "tasa_id", nullable = false, updatable = false)
  private Long tasaId;

  @Column(name = "valor_anterior", nullable = false, precision = 19, scale = 6, updatable = false)
  private BigDecimal valorAnterior;

  @Column(name = "valor_nuevo", nullable = false, precision = 19, scale = 6, updatable = false)
  private BigDecimal valorNuevo;

  @Column(name = "motivo", length = 300, updatable = false)
  private String motivo;

  @Column(name = "automatica", nullable = false, updatable = false)
  private boolean automatica;

  @Column(name = "corregida_por", updatable = false)
  private Long corregidaPor;

  @Column(name = "corregida_en", nullable = false, updatable = false)
  private Instant corregidaEn;

  protected CorreccionTasa() {}

  static CorreccionTasa manual(
      Long tasaId,
      BigDecimal anterior,
      BigDecimal nuevo,
      String motivo,
      Long usuarioId,
      Instant ahora) {
    CorreccionTasa correccion = new CorreccionTasa();
    correccion.tasaId = tasaId;
    correccion.valorAnterior = anterior;
    correccion.valorNuevo = nuevo;
    correccion.motivo = motivo == null || motivo.isBlank() ? null : motivo.trim();
    correccion.corregidaPor = usuarioId;
    correccion.corregidaEn = ahora;
    return correccion;
  }

  static CorreccionTasa automatica(
      Long tasaId, BigDecimal anterior, BigDecimal nuevo, String motivo, Instant ahora) {
    CorreccionTasa correccion = manual(tasaId, anterior, nuevo, motivo, null, ahora);
    correccion.automatica = true;
    return correccion;
  }

  public Long getId() {
    return id;
  }

  public Long getTasaId() {
    return tasaId;
  }

  public BigDecimal getValorAnterior() {
    return valorAnterior;
  }

  public BigDecimal getValorNuevo() {
    return valorNuevo;
  }

  public String getMotivo() {
    return motivo;
  }

  public boolean isAutomatica() {
    return automatica;
  }

  public Long getCorregidaPor() {
    return corregidaPor;
  }

  public Instant getCorregidaEn() {
    return corregidaEn;
  }
}
