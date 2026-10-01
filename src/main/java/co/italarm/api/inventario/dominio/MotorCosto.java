package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.Redondeo;
import java.math.BigDecimal;

/**
 * Regla de costo del inventario (sección 3.8, RF-66, RN-02). El costo se lleva en USD y se
 * recalcula con cada compra, comparando el costo unitario de la factura (ya en USD) con el costo
 * actual del producto en bodega.
 */
public final class MotorCosto {

  private MotorCosto() {}

  /**
   * @param stock unidades en bodega antes de la compra
   * @param costoActual costo unitario actual en USD, o null si nunca tuvo
   * @param cantidad unidades compradas
   * @param costoFacturaUsd costo unitario de la factura convertido a USD
   */
  public static ResultadoCosto aplicarCompra(
      BigDecimal stock, BigDecimal costoActual, BigDecimal cantidad, BigDecimal costoFacturaUsd) {
    if (costoActual == null || stock.signum() <= 0) {
      return new ResultadoCosto(
          costoActual, Redondeo.paraAlmacenar(costoFacturaUsd), ReglaCosto.SIN_STOCK);
    }
    if (costoFacturaUsd.compareTo(costoActual) > 0) {
      return new ResultadoCosto(
          costoActual, Redondeo.paraAlmacenar(costoFacturaUsd), ReglaCosto.SUBE);
    }
    BigDecimal valorEnBodega = stock.multiply(costoActual);
    BigDecimal valorComprado = cantidad.multiply(costoFacturaUsd);
    BigDecimal promedio = Redondeo.dividir(valorEnBodega.add(valorComprado), stock.add(cantidad));
    return new ResultadoCosto(costoActual, Redondeo.paraAlmacenar(promedio), ReglaCosto.PROMEDIO);
  }
}
