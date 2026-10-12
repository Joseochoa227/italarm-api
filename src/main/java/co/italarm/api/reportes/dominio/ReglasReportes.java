package co.italarm.api.reportes.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Porcentajes de los indicadores y del nivel de stock (RF-08, RF-10, RF-136, RF-142). */
public final class ReglasReportes {

  private static final BigDecimal CIEN = new BigDecimal("100");

  private ReglasReportes() {}

  /** parte ÷ total × 100 con 2 decimales; vacío si el total es 0. */
  public static BigDecimal porcentaje(BigDecimal parte, BigDecimal total) {
    if (total == null || total.signum() == 0 || parte == null) {
      return null;
    }
    return parte.multiply(CIEN).divide(total, 2, RoundingMode.HALF_UP);
  }

  /**
   * RF-142: aprobadas (incluidas las convertidas) sobre las que ya tuvieron respuesta del cliente
   * (aprobadas, convertidas y rechazadas). Las vencidas no tuvieron respuesta.
   */
  public static BigDecimal porcentajeAprobadas(long aprobadas, long rechazadas) {
    long respondidas = aprobadas + rechazadas;
    return respondidas == 0
        ? null
        : porcentaje(BigDecimal.valueOf(aprobadas), BigDecimal.valueOf(respondidas));
  }

  /** RF-10: nivel de la barra de stock, de 0 a 100 (stock ÷ mínimo). */
  public static BigDecimal nivelStock(BigDecimal stock, BigDecimal minimo) {
    if (minimo == null || minimo.signum() <= 0) {
      return CIEN;
    }
    BigDecimal nivel = porcentaje(stock.max(BigDecimal.ZERO), minimo);
    return nivel.min(CIEN);
  }
}
