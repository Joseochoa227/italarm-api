package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.Redondeo;
import java.math.BigDecimal;

/** Doble digitación de una tasa manual (RF-35a). */
public final class ConfirmacionTasa {

  private ConfirmacionTasa() {}

  /** Valida y devuelve la tasa con 6 decimales. */
  public static BigDecimal validar(BigDecimal valor, BigDecimal confirmacion) {
    if (valor == null || valor.signum() <= 0) {
      throw new TasaInvalidaException("La tasa debe ser mayor que 0.");
    }
    if (valor.stripTrailingZeros().scale() > Redondeo.ESCALA_CALCULO) {
      throw new TasaInvalidaException("La tasa admite máximo 6 decimales.");
    }
    if (confirmacion == null || valor.compareTo(confirmacion) != 0) {
      throw new TasaNoConfirmadaException();
    }
    return Redondeo.paraCalculo(valor);
  }
}
