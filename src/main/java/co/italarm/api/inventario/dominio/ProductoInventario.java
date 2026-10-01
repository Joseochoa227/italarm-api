package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.Redondeo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Stock y costo de un producto, vistos por el inventario. Es la misma tabla {@code producto} del
 * catálogo, pero el inventario solo maneja estas columnas. No toca la versión del producto, para
 * que una compra no genere conflicto con quien lo está editando.
 */
@Entity
@Table(name = "producto")
public class ProductoInventario {

  @Id private Long id;

  @Column(name = "stock", nullable = false, precision = 14, scale = 3)
  private BigDecimal stock;

  @Column(name = "costo_actual_usd", precision = 19, scale = 4)
  private BigDecimal costoActualUsd;

  protected ProductoInventario() {}

  public void entrar(BigDecimal cantidad) {
    this.stock = stock.add(cantidad);
  }

  /** Saca unidades sin dejar el stock en negativo (RF-62, RF-65). */
  public void salir(BigDecimal cantidad, String abreviatura) {
    ReglaStock.exigirDisponible(stock, cantidad, abreviatura);
    this.stock = stock.subtract(cantidad);
  }

  public void cambiarCosto(BigDecimal nuevoCosto) {
    this.costoActualUsd = nuevoCosto == null ? null : Redondeo.paraAlmacenar(nuevoCosto);
  }

  public Long getId() {
    return id;
  }

  public BigDecimal getStock() {
    return stock;
  }

  public BigDecimal getCostoActualUsd() {
    return costoActualUsd;
  }
}
