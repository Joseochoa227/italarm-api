package co.italarm.api.compras.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que pasaría al guardar la compra (RF-41, RF-42), sin guardar nada. Es el valor oficial que
 * muestra el frontend (BF-06).
 *
 * @param avisos por ejemplo, que la tasa usada no es la de la fecha de la compra (RF-33)
 */
public record VistaPreviaCompraVista(
    LocalDate fecha,
    Moneda moneda,
    TasasCompraVista tasas,
    List<String> avisos,
    List<Linea> lineas,
    MontoEnMonedas total) {

  /**
   * Cambio de costo de un producto.
   *
   * @param regla SUBE, PROMEDIO o SIN_STOCK
   */
  public record Linea(
      Long productoId,
      String codigo,
      String nombre,
      BigDecimal cantidad,
      String abreviatura,
      BigDecimal stockActual,
      Dinero costoUnitario,
      Dinero costoUnitarioUsd,
      Dinero costoActualUsd,
      Dinero costoNuevoUsd,
      String regla,
      MontoEnMonedas subtotal) {}
}
