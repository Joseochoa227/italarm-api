package co.italarm.api.ventas.dominio;

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
 * Producto vendido: cantidad, precio unitario en la moneda de la venta y costo en USD al momento de
 * la salida (RF-68). Guarda la descripción y la unidad para el comprobante.
 */
@Entity
@Table(name = "linea_venta")
public class LineaVenta {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "venta_id", nullable = false, updatable = false)
  private Venta venta;

  @Column(name = "producto_id", nullable = false, updatable = false)
  private Long productoId;

  @Column(name = "codigo", nullable = false, updatable = false, length = 30)
  private String codigo;

  @Column(name = "descripcion", nullable = false, updatable = false, length = 150)
  private String descripcion;

  @Column(name = "unidad", nullable = false, updatable = false, length = 10)
  private String unidad;

  @Column(name = "cantidad", nullable = false, updatable = false, precision = 14, scale = 3)
  private BigDecimal cantidad;

  @Column(name = "precio_unitario", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal precioUnitario;

  @Column(name = "precio_sugerido", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal precioSugerido;

  @Column(name = "subtotal", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal subtotal;

  @Column(
      name = "costo_unitario_usd",
      nullable = false,
      updatable = false,
      precision = 19,
      scale = 4)
  private BigDecimal costoUnitarioUsd;

  protected LineaVenta() {}

  public LineaVenta(
      Long productoId,
      String codigo,
      String descripcion,
      String unidad,
      BigDecimal cantidad,
      BigDecimal precioUnitario,
      BigDecimal precioSugerido,
      BigDecimal costoUnitarioUsd) {
    CalculoVenta.exigirPrecio(precioUnitario);
    this.productoId = productoId;
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

  void asignarVenta(Venta venta) {
    this.venta = venta;
  }

  public Long getId() {
    return id;
  }

  public Long getVentaId() {
    return venta.getId();
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
