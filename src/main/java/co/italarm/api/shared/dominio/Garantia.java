package co.italarm.api.shared.dominio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/** Garantías de equipos y mano de obra (RN-10): se cuentan en meses desde la salida. */
public final class Garantia {

  /** Días antes del vencimiento en que una garantía pasa a "Por vencer" (RF-113, P-43). */
  public static final int DIAS_POR_VENCER = 30;

  private Garantia() {}

  /**
   * Por vencer cuando faltan 30 días o menos (incluido el día del vencimiento); Vencida desde el
   * día siguiente (P-43).
   */
  public static EstadoGarantia estado(LocalDate vencimiento, LocalDate hoy) {
    if (hoy.isAfter(vencimiento)) {
      return EstadoGarantia.VENCIDA;
    }
    return hoy.isBefore(vencimiento.minusDays(DIAS_POR_VENCER))
        ? EstadoGarantia.VIGENTE
        : EstadoGarantia.POR_VENCER;
  }

  /** Días que faltan para el vencimiento; negativos si ya venció. */
  public static long diasRestantes(LocalDate vencimiento, LocalDate hoy) {
    return ChronoUnit.DAYS.between(hoy, vencimiento);
  }

  /** Fin de la garantía. Si el mes no tiene ese día, queda el último día del mes (CP-25). */
  public static LocalDate vencimiento(LocalDate desde, int meses) {
    return desde.plusMonths(meses);
  }
}
