package co.italarm.api.inventario.aplicacion;

import java.math.BigDecimal;

/** Formato de cantidades en las vistas del inventario. */
final class Cantidades {

  private Cantidades() {}

  /** Cantidad sin ceros sobrantes: 12.500 → 12.5; 3.000 → 3. */
  static BigDecimal sinCeros(BigDecimal valor) {
    if (valor == null) {
      return null;
    }
    BigDecimal sinCeros = valor.stripTrailingZeros();
    return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
  }
}
