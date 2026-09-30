package co.italarm.api.configuracion.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Datos de la empresa y valores por defecto del sistema (sección 3.17). Hay una sola fila. La
 * edición llega en la Fase 1.
 */
@Entity
@Table(name = "configuracion")
public class Configuracion extends EntidadMaestra {

  /** Identificador de la única fila de configuración. */
  public static final int ID_UNICO = 1;

  @Id
  @Column(name = "id")
  private Integer id;

  @Column(name = "empresa_nombre", nullable = false, length = 100)
  private String empresaNombre;

  @Column(name = "empresa_lema", length = 150)
  private String empresaLema;

  @Column(name = "empresa_nit", length = 30)
  private String empresaNit;

  @Column(name = "empresa_ciudad", length = 80)
  private String empresaCiudad;

  @Column(name = "empresa_telefono", length = 30)
  private String empresaTelefono;

  @Column(name = "empresa_correo", length = 254)
  private String empresaCorreo;

  @Column(name = "empresa_logo_clave", length = 300)
  private String empresaLogoClave;

  /** Porcentaje de variación que dispara la alerta al registrar una tasa manual (RF-35). */
  @Column(name = "limite_variacion_tasa", nullable = false, precision = 7, scale = 4)
  private BigDecimal limiteVariacionTasa;

  @Column(name = "validez_cotizacion_dias", nullable = false)
  private int validezCotizacionDias;

  @Column(name = "garantia_mano_obra_meses", nullable = false)
  private int garantiaManoObraMeses;

  @Column(name = "garantia_equipos_meses", nullable = false)
  private int garantiaEquiposMeses;

  @Column(name = "condiciones_garantia", nullable = false, columnDefinition = "text")
  private String condicionesGarantia;

  @Column(name = "pie_pdf", nullable = false, columnDefinition = "text")
  private String piePdf;

  protected Configuracion() {}

  public Integer getId() {
    return id;
  }

  public String getEmpresaNombre() {
    return empresaNombre;
  }

  public String getEmpresaLema() {
    return empresaLema;
  }

  public String getEmpresaNit() {
    return empresaNit;
  }

  public String getEmpresaCiudad() {
    return empresaCiudad;
  }

  public String getEmpresaTelefono() {
    return empresaTelefono;
  }

  public String getEmpresaCorreo() {
    return empresaCorreo;
  }

  public String getEmpresaLogoClave() {
    return empresaLogoClave;
  }

  public BigDecimal getLimiteVariacionTasa() {
    return limiteVariacionTasa;
  }

  public int getValidezCotizacionDias() {
    return validezCotizacionDias;
  }

  public int getGarantiaManoObraMeses() {
    return garantiaManoObraMeses;
  }

  public int getGarantiaEquiposMeses() {
    return garantiaEquiposMeses;
  }

  public String getCondicionesGarantia() {
    return condicionesGarantia;
  }

  public String getPiePdf() {
    return piePdf;
  }
}
