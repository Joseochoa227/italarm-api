package co.italarm.api.shared.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Totales, costo y utilidad de una venta o instalación (RF-100, RF-119, RN-03, RF-68). El costo de
 * cada producto está en USD y se convierte a la moneda del documento con la tasa guardada en él
 * (RN-04). La mano de obra no tiene costo de material: suma completa a la utilidad.
 */
public final class CalculoDocumento {

  private static final BigDecimal CIEN = new BigDecimal("100");

  private CalculoDocumento() {}

  /** Cantidad, precio unitario en la moneda del documento y costo unitario en USD. */
  public record Linea(BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal costoUnitarioUsd) {

    public BigDecimal subtotal() {
      return Redondeo.paraAlmacenar(cantidad.multiply(precioUnitario));
    }
  }

  /** Venta: solo material. */
  public static ResumenDocumento calcular(
      List<Linea> lineas, Descuento descuento, Moneda moneda, Tasas tasas) {
    return calcular(lineas, BigDecimal.ZERO, descuento, moneda, tasas);
  }

  /** Instalación: material + mano de obra − descuento (RN-03). */
  public static ResumenDocumento calcular(
      List<Linea> lineas, BigDecimal manoDeObra, Descuento descuento, Moneda moneda, Tasas tasas) {
    if (manoDeObra == null || manoDeObra.signum() < 0) {
      throw new PrecioInvalidoException("La mano de obra no puede ser negativa.");
    }
    BigDecimal material = BigDecimal.ZERO;
    BigDecimal costoUsd = BigDecimal.ZERO;
    for (Linea linea : lineas) {
      exigirPrecio(linea.precioUnitario());
      material = material.add(linea.subtotal());
      BigDecimal costo =
          linea.costoUnitarioUsd() == null ? BigDecimal.ZERO : linea.costoUnitarioUsd();
      costoUsd = costoUsd.add(linea.cantidad().multiply(costo));
    }
    BigDecimal subtotal = material.add(Redondeo.paraAlmacenar(manoDeObra));
    BigDecimal montoDescuento = descuento.monto(subtotal);
    BigDecimal total = subtotal.subtract(montoDescuento);
    BigDecimal costo = Redondeo.paraAlmacenar(tasas.desdeUsd(costoUsd, moneda));
    BigDecimal utilidad = total.subtract(costo);
    BigDecimal totalUsd = Redondeo.paraAlmacenar(tasas.aUsd(total, moneda));
    BigDecimal costoUsdGuardado = Redondeo.paraAlmacenar(costoUsd);
    return new ResumenDocumento(
        Redondeo.paraAlmacenar(material),
        Redondeo.paraAlmacenar(manoDeObra),
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
      throw new PrecioInvalidoException("El precio no puede ser negativo.");
    }
  }
}
