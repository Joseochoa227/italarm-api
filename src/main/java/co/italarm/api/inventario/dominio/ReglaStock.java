package co.italarm.api.inventario.dominio;

import java.math.BigDecimal;

/** El stock nunca queda en negativo (RF-62, RF-65, RN-05). */
public final class ReglaStock {

  private ReglaStock() {}

  public static void exigirDisponible(BigDecimal stock, BigDecimal salida, String abreviatura) {
    if (salida.compareTo(stock) > 0) {
      throw new StockInsuficienteException(
          "Stock insuficiente · quedan "
              + stock.stripTrailingZeros().toPlainString()
              + " "
              + abreviatura);
    }
  }
}
