package co.italarm.api.cotizaciones.dominio;

import co.italarm.api.shared.dominio.CalculoDocumento;
import co.italarm.api.shared.dominio.Redondeo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Producto cotizado: cantidad, precio unitario en la moneda de la cotización, precio sugerido y
 * costo en USD al cotizar, para avisar al convertir si cambiaron (RF-96).
 */
@Entity
@Table(name = "linea_cotizacion")
public class LineaCotizacion {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cotizacion_id", nullable = false, updatable = false)
  private Cotizacion cotizacion;

  @Column(name = "producto_id", nullable = false, updatable = false)
  private Long productoId;

  @Column(name = "codigo", nullable = false, length = 30)
  private String codigo;

  @Column(name = "descripcion", nullable = false, length = 150)
  private String descripcion;

  @Column(name = "unidad", nullable = false, length = 10)
  private String unidad;

  @Column(name = "cantidad", nullable = false, precision = 14, scale = 3)
  private BigDecimal cantidad;

  @Column(name = "precio_unitario", nullable = false, precision = 19, scale = 4)
  private BigDecimal precioUnitario;

  @Column(name = "precio_sugerido", nullable = false, precision = 19, scale = 4)
  private BigDecimal precioSugerido;

  @Column(name = "subtotal", nullable = false, precision = 19, scale = 4)
  private BigDecimal subtotal;

  @Column(name = "costo_unitario_usd", nullable = false, precision = 19, scale = 4)
  private BigDecimal costoUnitarioUsd;

  protected LineaCotizacion() {}

  public LineaCotizacion(
      Long productoId,
      String codigo,
      String descripcion,
      String unidad,
      BigDecimal cantidad,
      BigDecimal precioUnitario,
      BigDecimal precioSugerido,
      BigDecimal costoUnitarioUsd) {
    this.productoId = productoId;
    asignar(
        codigo, descripcion, unidad, cantidad, precioUnitario, precioSugerido, costoUnitarioUsd);
  }

  private void asignar(
      String codigo,
      String descripcion,
      String unidad,
      BigDecimal cantidad,
      BigDecimal precioUnitario,
      BigDecimal precioSugerido,
      BigDecimal costoUnitarioUsd) {
    CalculoDocumento.exigirPrecio(precioUnitario);
    this.codigo = codigo;
    this.descripcion = descripcion;
    this.unidad = unidad;
    this.cantidad = cantidad;
    this.precioUnitario = Redondeo.paraAlmacenar(precioUnitario);
    this.precioSugerido = Redondeo.paraAlmacenar(precioSugerido);
    this.subtotal = Redondeo.paraAlmacenar(cantidad.multiply(precioUnitario));
    this.costoUnitarioUsd =
        Redondeo.paraAlmacenar(costoUnitarioUsd == null ? BigDecimal.ZERO : costoUnitarioUsd);
  }

  /** Al editar, la línea del mismo producto toma los valores de la nueva. */
  void actualizarDesde(LineaCotizacion nueva) {
    asignar(
        nueva.codigo,
        nueva.descripcion,
        nueva.unidad,
        nueva.cantidad,
        nueva.precioUnitario,
        nueva.precioSugerido,
        nueva.costoUnitarioUsd);
  }

  void asignarCotizacion(Cotizacion cotizacion) {
    this.cotizacion = cotizacion;
  }

  public Long getId() {
    return id;
  }

  public Long getCotizacionId() {
    return cotizacion.getId();
  }

  public Long getProductoId() {
    return productoId;
  }

  public String getCodigo() {
    return codigo;
  }

  public String getDescripcion() {
    return descripcion;
  }

  public String getUnidad() {
    return unidad;
  }

  public BigDecimal getCantidad() {
    return cantidad;
  }

  public BigDecimal getPrecioUnitario() {
    return precioUnitario;
  }

  public BigDecimal getPrecioSugerido() {
    return precioSugerido;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public BigDecimal getCostoUnitarioUsd() {
    return costoUnitarioUsd;
  }
}
