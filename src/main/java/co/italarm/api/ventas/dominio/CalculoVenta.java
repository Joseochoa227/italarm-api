package co.italarm.api.ventas.dominio;

import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.Redondeo;
import co.italarm.api.shared.dominio.Tasas;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Totales, costo y utilidad de una venta (RF-100, RN-03, RF-68). El costo de cada producto está en
 * USD y se convierte a la moneda de la venta con la tasa guardada en ella (RN-04).
 */
public final class CalculoVenta {

  private static final BigDecimal CIEN = new BigDecimal("100");

  private CalculoVenta() {}

  /** Cantidad, precio unitario en la moneda de la venta y costo unitario en USD. */
  public record Linea(BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal costoUnitarioUsd) {

    public BigDecimal subtotal() {
      return Redondeo.paraAlmacenar(cantidad.multiply(precioUnitario));
    }
  }

  public static ResumenVenta calcular(
      List<Linea> lineas, Descuento descuento, Moneda moneda, Tasas tasas) {
    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal costoUsd = BigDecimal.ZERO;
    for (Linea linea : lineas) {
      exigirPrecio(linea.precioUnitario());
      subtotal = subtotal.add(linea.subtotal());
      BigDecimal costo =
          linea.costoUnitarioUsd() == null ? BigDecimal.ZERO : linea.costoUnitarioUsd();
      costoUsd = costoUsd.add(linea.cantidad().multiply(costo));
    }
    BigDecimal montoDescuento = descuento.monto(subtotal);
    BigDecimal total = subtotal.subtract(montoDescuento);
    BigDecimal costo = Redondeo.paraAlmacenar(tasas.desdeUsd(costoUsd, moneda));
    BigDecimal utilidad = total.subtract(costo);
    BigDecimal totalUsd = Redondeo.paraAlmacenar(tasas.aUsd(total, moneda));
    BigDecimal costoUsdGuardado = Redondeo.paraAlmacenar(costoUsd);
    return new ResumenVenta(
        Redondeo.paraAlmacenar(subtotal),
        montoDescuento,
        Redondeo.paraAlmacenar(total),
        costo,
        Redondeo.paraAlmacenar(utilidad),
        total.signum() == 0 ? null : utilidad.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP),
        totalUsd,
        costoUsdGuardado,
        totalUsd.subtract(costoUsdGuardado));
  }

  /** P-29: el precio puede ser 0 o mayor, nunca negativo. */
  public static void exigirPrecio(BigDecimal precio) {
    if (precio == null || precio.signum() < 0) {
      throw new VentaInvalidaException(
          VentaInvalidaException.PRECIO_INVALIDO, "El precio no puede ser negativo.");
    }
  }
}
