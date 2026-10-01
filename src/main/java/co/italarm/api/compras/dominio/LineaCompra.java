package co.italarm.api.compras.dominio;

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
 * Producto de una compra: cantidad, costo unitario en la moneda de la factura y en USD, y el cambio
 * de costo que produjo (RF-41, RF-42). Una línea por producto (P-20).
 */
@Entity
@Table(name = "linea_compra")
public class LineaCompra {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "compra_id", nullable = false, updatable = false)
  private Compra compra;

  @Column(name = "producto_id", nullable = false, updatable = false)
  private Long productoId;

  @Column(name = "cantidad", nullable = false, updatable = false, precision = 14, scale = 3)
  private BigDecimal cantidad;

  @Column(name = "costo_unitario", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal costoUnitario;

  @Column(
      name = "costo_unitario_usd",
      nullable = false,
      updatable = false,
      precision = 19,
      scale = 6)
  private BigDecimal costoUnitarioUsd;

  @Column(name = "subtotal", nullable = false, updatable = false, precision = 19, scale = 4)
  private BigDecimal subtotal;

  @Column(name = "costo_anterior_usd", precision = 19, scale = 4)
  private BigDecimal costoAnteriorUsd;

  @Column(name = "costo_nuevo_usd", precision = 19, scale = 4)
  private BigDecimal costoNuevoUsd;

  @Column(name = "regla", length = 20)
  private String regla;

  protected LineaCompra() {}

  public LineaCompra(
      Long productoId, BigDecimal cantidad, BigDecimal costoUnitario, BigDecimal costoUnitarioUsd) {
    this.productoId = productoId;
    this.cantidad = cantidad;
    this.costoUnitario = Redondeo.paraAlmacenar(costoUnitario);
    this.costoUnitarioUsd = Redondeo.paraCalculo(costoUnitarioUsd);
    this.subtotal = Redondeo.paraAlmacenar(cantidad.multiply(costoUnitario));
  }

  /** Registra el cambio de costo que produjo la línea; se hace una sola vez, al guardar. */
  public void registrarCambioCosto(BigDecimal anterior, BigDecimal nuevo, String reglaAplicada) {
    if (this.regla != null) {
      throw new IllegalStateException("El cambio de costo de la línea ya estaba registrado");
    }
    this.costoAnteriorUsd = anterior;
    this.costoNuevoUsd = nuevo;
    this.regla = reglaAplicada;
  }

  void asignarCompra(Compra compra) {
    this.compra = compra;
  }

  public Long getId() {
    return id;
  }

  public Long getProductoId() {
    return productoId;
  }

  public BigDecimal getCantidad() {
    return cantidad;
  }

  public BigDecimal getCostoUnitario() {
    return costoUnitario;
  }

  public BigDecimal getCostoUnitarioUsd() {
    return costoUnitarioUsd;
  }

  public BigDecimal getSubtotal() {
    return subtotal;
  }

  public BigDecimal getCostoAnteriorUsd() {
    return costoAnteriorUsd;
  }

  public BigDecimal getCostoNuevoUsd() {
    return costoNuevoUsd;
  }

  public String getRegla() {
    return regla;
  }
}
