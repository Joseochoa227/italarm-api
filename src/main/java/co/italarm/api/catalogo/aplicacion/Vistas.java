package co.italarm.api.catalogo.aplicacion;

import java.math.BigDecimal;

/** Utilidades para construir las vistas del catálogo. */
final class Vistas {

  private Vistas() {}

  /** Cantidad sin ceros sobrantes: 12.500 → 12.5; 0.000 → 0. */
  static BigDecimal cantidad(BigDecimal valor) {
    if (valor == null) {
      return null;
    }
    BigDecimal sinCeros = valor.stripTrailingZeros();
    return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
  }
}
