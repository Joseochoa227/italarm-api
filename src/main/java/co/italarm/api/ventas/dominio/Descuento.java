package co.italarm.api.ventas.dominio;

import co.italarm.api.shared.dominio.Redondeo;
import java.math.BigDecimal;

/**
 * Descuento de toda la venta (RN-12, P-30): un porcentaje del subtotal o un valor en la moneda de
 * la venta.
 */
public record Descuento(TipoDescuento tipo, BigDecimal valor) {

  private static final BigDecimal CIEN = new BigDecimal("100");

  public Descuento {
    if (valor.signum() < 0) {
      throw new DescuentoInvalidoException("El descuento no puede ser negativo.");
    }
    if (tipo == TipoDescuento.PORCENTAJE && valor.compareTo(CIEN) > 0) {
      throw new DescuentoInvalidoException("El descuento no puede ser mayor que el 100 %.");
    }
  }

  /** Sin tipo o sin valor, no hay descuento. */
  public static Descuento de(TipoDescuento tipo, BigDecimal valor) {
    return tipo == null || valor == null ? ninguno() : new Descuento(tipo, valor);
  }

  public static Descuento ninguno() {
    return new Descuento(TipoDescuento.VALOR, BigDecimal.ZERO);
  }

  /** Valor del descuento sobre el subtotal; nunca mayor que el subtotal. */
  public BigDecimal monto(BigDecimal subtotal) {
    BigDecimal monto =
        Redondeo.paraAlmacenar(
            tipo == TipoDescuento.PORCENTAJE
                ? subtotal.multiply(valor).divide(CIEN, Redondeo.ESCALA_CALCULO, Redondeo.MODO)
                : valor);
    if (monto.compareTo(subtotal) > 0) {
      throw new DescuentoInvalidoException("El descuento no puede ser mayor que el subtotal.");
    }
    return monto;
  }
}
