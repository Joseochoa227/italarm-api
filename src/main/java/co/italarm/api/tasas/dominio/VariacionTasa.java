package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.Redondeo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Variación de una tasa nueva frente a la anterior (RF-35b) y si supera el límite configurado
 * (RF-35c). Sin tasa anterior no hay variación.
 *
 * @param porcentaje variación en porcentaje con 2 decimales; positiva si sube, negativa si baja
 */
public record VariacionTasa(
    BigDecimal anterior,
    BigDecimal nueva,
    BigDecimal porcentaje,
    BigDecimal limite,
    boolean superaLimite) {

  private static final BigDecimal CIEN = new BigDecimal("100");

  public static VariacionTasa calcular(BigDecimal anterior, BigDecimal nueva, BigDecimal limite) {
    if (anterior == null || anterior.signum() == 0) {
      return new VariacionTasa(anterior, nueva, null, limite, false);
    }
    BigDecimal exacta = Redondeo.dividir(nueva.subtract(anterior).multiply(CIEN), anterior);
    return new VariacionTasa(
        anterior,
        nueva,
        exacta.setScale(2, RoundingMode.HALF_UP),
        limite,
        exacta.abs().compareTo(limite) > 0);
  }

  /** Si supera el límite, exige que el usuario la haya aceptado expresamente. */
  public void exigirAceptacion(boolean aceptada) {
    if (superaLimite && !aceptada) {
      throw new TasaVariacionNoAceptadaException(
          "La nueva tasa varía "
              + formato("0.00").format(porcentaje)
              + " % frente a la anterior (límite "
              + formato("0.##").format(limite)
              + " %). Confírmala expresamente para guardarla.");
    }
  }

  private static DecimalFormat formato(String patron) {
    DecimalFormatSymbols simbolos = new DecimalFormatSymbols();
    simbolos.setDecimalSeparator(',');
    simbolos.setMinusSign('-');
    return new DecimalFormat(patron, simbolos);
  }
}
