package co.italarm.api.shared.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Precio que se propone en una línea (RN-01, RF-78): el de lista del producto según el tipo de
 * cliente, convertido a la moneda de la venta con las tasas del día (P-28). En COP se redondea a
 * pesos enteros; en USD y VES, a 2 decimales.
 */
public final class PrecioSugerido {

  private PrecioSugerido() {}

  public static BigDecimal en(Dinero precioLista, Moneda monedaVenta, Tasas tasas) {
    if (precioLista.moneda() == monedaVenta) {
      return precioLista.monto();
    }
    BigDecimal convertido =
        tasas.desdeUsd(tasas.aUsd(precioLista.monto(), precioLista.moneda()), monedaVenta);
    return convertido.setScale(monedaVenta == Moneda.COP ? 0 : 2, RoundingMode.HALF_UP);
  }
}
