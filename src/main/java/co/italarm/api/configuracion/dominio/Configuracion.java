package co.italarm.api.configuracion.dominio;

import co.italarm.api.shared.dominio.EntidadMaestra;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.Textos;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Set;

/**
 * Datos de la empresa y valores por defecto del sistema (sección 3.17). Hay una sola fila. La
 * edición llega en la Fase 1.
 */
@Entity
@Table(name = "configuracion")
public class Configuracion extends EntidadMaestra {

  /** Identificador de la única fila de configuración. */
  public static final int ID_UNICO = 1;

  private static final Set<Integer> VALIDECES_PERMITIDAS = Set.of(8, 15, 30);
  private static final BigDecimal CIEN = new BigDecimal("100");

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

  /**
   * Aplica los datos de la empresa y los valores por defecto. Validez de 8, 15 o 30 días (RF-83);
   * garantías de 1 a 3 meses (RF-123); límite de variación mayor que 0 % y hasta 100 %.
   */
  public void actualizar(DatosConfiguracion datos) {
    if (!VALIDECES_PERMITIDAS.contains(datos.validezCotizacionDias())) {
      throw new ConfiguracionInvalidaException(
          "La validez de las cotizaciones debe ser de 8, 15 o 30 días.");
    }
    if (datos.garantiaManoObraMeses() < 1 || datos.garantiaManoObraMeses() > 3) {
      throw new ConfiguracionInvalidaException(
          "La garantía de mano de obra debe ser de 1 a 3 meses.");
    }
    if (datos.garantiaEquiposMeses() < 1 || datos.garantiaEquiposMeses() > 3) {
      throw new ConfiguracionInvalidaException("La garantía de equipos debe ser de 1 a 3 meses.");
    }
    BigDecimal limite = datos.limiteVariacionTasa();
    if (limite == null || limite.signum() <= 0 || limite.compareTo(CIEN) > 0) {
      throw new ConfiguracionInvalidaException(
          "El límite de variación de tasas debe ser mayor que 0 % y máximo 100 %.");
    }
    this.empresaNombre = Textos.limpiar(datos.empresaNombre());
    this.empresaLema = Textos.limpiar(datos.empresaLema());
    this.empresaNit = Textos.limpiar(datos.empresaNit());
    this.empresaCiudad = Textos.limpiar(datos.empresaCiudad());
    this.empresaTelefono = Textos.limpiar(datos.empresaTelefono());
    this.empresaCorreo = Textos.limpiar(datos.empresaCorreo());
    this.limiteVariacionTasa = Redondeo.paraAlmacenar(limite);
    this.validezCotizacionDias = datos.validezCotizacionDias();
    this.garantiaManoObraMeses = datos.garantiaManoObraMeses();
    this.garantiaEquiposMeses = datos.garantiaEquiposMeses();
    this.condicionesGarantia = datos.condicionesGarantia().trim();
    this.piePdf = datos.piePdf().trim();
  }

  /** Asigna el nuevo logo y devuelve la clave del anterior (para eliminarlo), o null. */
  public String cambiarLogo(String nuevaClave) {
    String anterior = empresaLogoClave;
    this.empresaLogoClave = nuevaClave;
    return anterior;
  }

  /** Quita el logo y devuelve su clave (para eliminarlo), o null. */
  public String quitarLogo() {
    return cambiarLogo(null);
  }

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
