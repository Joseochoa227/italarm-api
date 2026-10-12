package co.italarm.api.documentos.aplicacion;

import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * Contenido de una exportación a Excel (RF-144, RNF-06): una o varias hojas con títulos, columnas y
 * filas tipadas. Los montos van como números con el formato de su moneda, para poder sumarlos
 * (RNF-04, P-62).
 *
 * @param nombre nombre del archivo sin extensión, por ejemplo "ventas"
 */
public record LibroExcel(String nombre, List<Hoja> hojas) {

  /**
   * Una hoja.
   *
   * @param nombre nombre de la pestaña (máximo 31 caracteres)
   * @param titulo por ejemplo "Ventas"
   * @param filtros líneas que describen los filtros o el período
   */
  public record Hoja(
      String nombre,
      String titulo,
      List<String> filtros,
      List<String> columnas,
      List<List<Celda>> filas) {}

  /** Valor de una celda. Una celda vacía es {@code null}. */
  public sealed interface Celda {}

  public record Texto(String valor) implements Celda {}

  /** Número sin moneda (cantidades, porcentajes). */
  public record Numero(BigDecimal valor) implements Celda {}

  /** Monto con el formato de su moneda: COP sin decimales; USD y VES con 2 (RNF-04). */
  public record Monto(BigDecimal valor, Moneda moneda) implements Celda {}

  public record Fecha(LocalDate valor) implements Celda {}

  /** Fecha y hora mostradas en hora de Colombia. */
  public record FechaHora(Instant valor) implements Celda {}

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  /** Fila con celdas que pueden ser vacías. */
  public static List<Celda> fila(Celda... celdas) {
    return Arrays.asList(celdas);
  }

  /** "Período: 01/10/2026 al 31/10/2026". */
  public static String periodo(LocalDate desde, LocalDate hasta) {
    return "Período: " + FECHA.format(desde) + " al " + FECHA.format(hasta);
  }

  public static Celda texto(String valor) {
    return valor == null ? null : new Texto(valor);
  }

  public static Celda numero(BigDecimal valor) {
    return valor == null ? null : new Numero(valor);
  }

  public static Celda monto(BigDecimal valor, Moneda moneda) {
    return valor == null ? null : new Monto(valor, moneda);
  }

  public static Celda dinero(co.italarm.api.shared.dominio.Dinero dinero) {
    return dinero == null ? null : new Monto(dinero.monto(), dinero.moneda());
  }

  public static Celda fecha(LocalDate valor) {
    return valor == null ? null : new Fecha(valor);
  }

  public static Celda fechaHora(Instant valor) {
    return valor == null ? null : new FechaHora(valor);
  }
}
