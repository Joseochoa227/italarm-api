package co.italarm.api.shared.dominio;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Formato de dinero para Colombia (RNF-04): {@code $ 1.250.000}, {@code US$ 1.939,04} y {@code Bs
 * 1.234,56}. Se usa en PDF y Excel; en la API el dinero viaja sin formato.
 */
public final class FormatoDinero {

  private FormatoDinero() {}

  public static String formatear(Dinero dinero) {
    BigDecimal valor = Redondeo.paraMostrar(dinero.monto(), dinero.moneda());
    String signo = valor.signum() < 0 ? "-" : "";
    return signo + dinero.moneda().simbolo() + " " + formato(dinero.moneda()).format(valor.abs());
  }

  private static DecimalFormat formato(Moneda moneda) {
    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
    simbolos.setGroupingSeparator('.');
    simbolos.setDecimalSeparator(',');
    String patron =
        moneda.decimalesVisibles() == 0
            ? "#,##0"
            : "#,##0." + "0".repeat(moneda.decimalesVisibles());
    DecimalFormat formato = new DecimalFormat(patron, simbolos);
    formato.setRoundingMode(Redondeo.MODO);
    return formato;
  }
}
