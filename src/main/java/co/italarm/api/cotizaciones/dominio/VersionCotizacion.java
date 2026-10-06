package co.italarm.api.cotizaciones.dominio;

import co.italarm.api.shared.dominio.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Versión anterior de una cotización, guardada al editarla en evaluación (RF-88, P-46). */
@Entity
@Table(name = "version_cotizacion")
public class VersionCotizacion extends EntidadAuditable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "cotizacion_id", nullable = false, updatable = false)
  private Long cotizacionId;

  @Column(name = "numero_version", nullable = false, updatable = false)
  private int numeroVersion;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "contenido", nullable = false, updatable = false)
  private ContenidoVersion contenido;

  protected VersionCotizacion() {}

  VersionCotizacion(Long cotizacionId, int numeroVersion, ContenidoVersion contenido) {
    this.cotizacionId = cotizacionId;
    this.numeroVersion = numeroVersion;
    this.contenido = contenido;
  }

  public Long getId() {
    return id;
  }

  public Long getCotizacionId() {
    return cotizacionId;
  }

  public int getNumeroVersion() {
    return numeroVersion;
  }

  public ContenidoVersion getContenido() {
    return contenido;
  }
}
