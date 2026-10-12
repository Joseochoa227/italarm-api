package co.italarm.api.reportes.dominio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Período de un reporte (RF-135, P-60): semana de lunes a domingo, mes, año o rango libre, con los
 * tramos del gráfico de barras (RF-137).
 */
public record Periodo(TipoPeriodo tipo, LocalDate desde, LocalDate hasta) {

  /**
   * P-60: un rango libre de hasta este número de días se agrupa por semana; si es mayor, por mes.
   */
  public static final int DIAS_RANGO_POR_SEMANA = 92;

  /** Un rango libre no puede pasar de 10 años. */
  public static final int DIAS_MAXIMOS = 3_660;

  private static final Locale ESPANOL = Locale.forLanguageTag("es-CO");
  private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

  /** Agrupación de las barras del gráfico. */
  public enum Agrupacion {
    SEMANA,
    MES
  }

  /** Una barra del gráfico: sus días dentro del período. */
  public record Tramo(String etiqueta, LocalDate desde, LocalDate hasta) {}

  /** La semana, el mes o el año que contiene la fecha. */
  public static Periodo de(TipoPeriodo tipo, LocalDate fecha) {
    return switch (tipo) {
      case SEMANA -> {
        LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        yield new Periodo(tipo, lunes, lunes.plusDays(6));
      }
      case MES -> new Periodo(tipo, fecha.withDayOfMonth(1), YearMonth.from(fecha).atEndOfMonth());
      case ANIO ->
          new Periodo(tipo, fecha.withDayOfYear(1), fecha.withDayOfYear(fecha.lengthOfYear()));
      case RANGO -> throw new PeriodoInvalidoException("Un rango libre necesita desde y hasta.");
    };
  }

  /** Rango libre de fechas, ambas incluidas. */
  public static Periodo rango(LocalDate desde, LocalDate hasta) {
    if (desde == null || hasta == null) {
      throw new PeriodoInvalidoException("Indica la fecha inicial y la final del rango.");
    }
    if (hasta.isBefore(desde)) {
      throw new PeriodoInvalidoException("La fecha final no puede ser anterior a la inicial.");
    }
    if (ChronoUnit.DAYS.between(desde, hasta) >= DIAS_MAXIMOS) {
      throw new PeriodoInvalidoException("El rango no puede pasar de 10 años.");
    }
    return new Periodo(TipoPeriodo.RANGO, desde, hasta);
  }

  /** El período anterior, para la flecha (RF-135); vacío en un rango libre. */
  public Periodo anterior() {
    return switch (tipo) {
      case SEMANA -> de(tipo, desde.minusWeeks(1));
      case MES -> de(tipo, desde.minusMonths(1));
      case ANIO -> de(tipo, desde.minusYears(1));
      case RANGO -> null;
    };
  }

  /** El período siguiente, para la flecha (RF-135); vacío en un rango libre. */
  public Periodo siguiente() {
    return switch (tipo) {
      case SEMANA -> de(tipo, desde.plusWeeks(1));
      case MES -> de(tipo, desde.plusMonths(1));
      case ANIO -> de(tipo, desde.plusYears(1));
      case RANGO -> null;
    };
  }

  public long dias() {
    return ChronoUnit.DAYS.between(desde, hasta) + 1;
  }

  /** P-60: el año (y un rango de más de 3 meses) va por mes; lo demás, por semana. */
  public Agrupacion agrupacion() {
    if (tipo == TipoPeriodo.ANIO || (tipo == TipoPeriodo.RANGO && dias() > DIAS_RANGO_POR_SEMANA)) {
      return Agrupacion.MES;
    }
    return Agrupacion.SEMANA;
  }

  /**
   * Barras del gráfico (RF-137). Las semanas o meses partidos al inicio o al final del período
   * cuentan solo sus días dentro del período (P-60).
   */
  public List<Tramo> tramos() {
    List<Tramo> tramos = new ArrayList<>();
    if (agrupacion() == Agrupacion.MES) {
      for (YearMonth mes = YearMonth.from(desde);
          !mes.isAfter(YearMonth.from(hasta));
          mes = mes.plusMonths(1)) {
        LocalDate inicio = max(mes.atDay(1), desde);
        LocalDate fin = min(mes.atEndOfMonth(), hasta);
        String nombre = mes.getMonth().getDisplayName(TextStyle.FULL, ESPANOL);
        tramos.add(
            new Tramo(
                Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + mes.getYear(),
                inicio,
                fin));
      }
      return tramos;
    }
    for (LocalDate lunes = desde.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        !lunes.isAfter(hasta);
        lunes = lunes.plusWeeks(1)) {
      LocalDate inicio = max(lunes, desde);
      LocalDate fin = min(lunes.plusDays(6), hasta);
      tramos.add(new Tramo(DIA_MES.format(inicio) + " al " + DIA_MES.format(fin), inicio, fin));
    }
    return tramos;
  }

  public boolean contiene(LocalDate fecha) {
    return !fecha.isBefore(desde) && !fecha.isAfter(hasta);
  }

  private static LocalDate max(LocalDate a, LocalDate b) {
    return a.isAfter(b) ? a : b;
  }

  private static LocalDate min(LocalDate a, LocalDate b) {
    return a.isBefore(b) ? a : b;
  }
}
