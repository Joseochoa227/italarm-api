package co.italarm.api.instalaciones.dominio;

import co.italarm.api.shared.dominio.EntidadAuditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Foto de una instalación (RF-110). El archivo vive en el almacenamiento; aquí, su clave. */
@Entity
@Table(name = "foto_instalacion")
public class FotoInstalacion extends EntidadAuditable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "instalacion_id", nullable = false, updatable = false)
  private Long instalacionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "grupo", nullable = false, updatable = false, length = 10)
  private GrupoFoto grupo;

  @Column(name = "clave", nullable = false, updatable = false, length = 300)
  private String clave;

  protected FotoInstalacion() {}

  /**
   * @param fotosEnElGrupo cuántas fotos tiene ya el grupo (P-42)
   */
  public static FotoInstalacion agregar(
      Long instalacionId, GrupoFoto grupo, String clave, long fotosEnElGrupo) {
    if (fotosEnElGrupo >= ReglasInstalacion.FOTOS_POR_GRUPO) {
      throw new FotosMaximasException(grupo);
    }
    FotoInstalacion foto = new FotoInstalacion();
    foto.instalacionId = instalacionId;
    foto.grupo = grupo;
    foto.clave = clave;
    return foto;
  }

  public Long getId() {
    return id;
  }

  public Long getInstalacionId() {
    return instalacionId;
  }

  public GrupoFoto getGrupo() {
    return grupo;
  }

  public String getClave() {
    return clave;
  }
}
