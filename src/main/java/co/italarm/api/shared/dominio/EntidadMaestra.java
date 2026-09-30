package co.italarm.api.shared.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;

/** Dato maestro: auditado y con control de versión optimista (BP-12). */
@MappedSuperclass
public abstract class EntidadMaestra extends EntidadAuditable {

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  public long getVersion() {
    return version;
  }
}
