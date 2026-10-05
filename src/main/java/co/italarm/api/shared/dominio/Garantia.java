package co.italarm.api.shared.dominio;

import java.time.LocalDate;

/** Garantías de equipos y mano de obra (RN-10): se cuentan en meses desde la salida. */
public final class Garantia {

  private Garantia() {}

  /** Fin de la garantía. Si el mes no tiene ese día, queda el último día del mes (CP-25). */
  public static LocalDate vencimiento(LocalDate desde, int meses) {
    return desde.plusMonths(meses);
  }
}
