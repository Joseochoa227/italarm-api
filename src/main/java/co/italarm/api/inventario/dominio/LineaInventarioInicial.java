package co.italarm.api.inventario.dominio;

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

/** Producto cargado en un inventario inicial, con su cantidad y costo en USD. */
@Entity
@Table(name = "linea_inventario_inicial")
public class LineaInventarioInicial {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "inventario_inicial_id", nullable = false, updatable = false)
  private InventarioInicial documento;

  @Column(name = "producto_id", nullable = false, updatable = false)
  private Long productoId;

  @Column(name = "cantidad", nullable = false, updatable = false, precision = 14, scale = 3)
  private BigDecimal cantidad;

  @Column(
      name = "costo_unitario_usd",
      nullable = false,
      updatable = false,
      precision = 19,
      scale = 4)
  private BigDecimal costoUnitarioUsd;

  protected LineaInventarioInicial() {}

  public LineaInventarioInicial(Long productoId, BigDecimal cantidad, BigDecimal costoUnitarioUsd) {
    this.productoId = productoId;
    this.cantidad = cantidad;
    this.costoUnitarioUsd = costoUnitarioUsd;
  }

  void asignarDocumento(InventarioInicial documento) {
    this.documento = documento;
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

  public BigDecimal getCostoUnitarioUsd() {
    return costoUnitarioUsd;
  }
}
