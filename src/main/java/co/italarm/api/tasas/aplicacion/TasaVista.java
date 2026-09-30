package co.italarm.api.tasas.aplicacion;

import co.italarm.api.tasas.dominio.FuenteTasa;
import co.italarm.api.tasas.dominio.ParMoneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Tasa del historial (RF-34), con sus correcciones cuando se consulta el detalle. */
public record TasaVista(
    Long id,
    ParMoneda par,
    LocalDate fecha,
    BigDecimal valor,
    FuenteTasa fuente,
    Instant registradaEn,
    String registradaPor,
    List<CorreccionVista> correcciones) {

  /** Corrección de la tasa (RF-36). {@code automatica}: la TRM oficial reemplazó a la manual. */
  public record CorreccionVista(
      BigDecimal valorAnterior,
      BigDecimal valorNuevo,
      String motivo,
      boolean automatica,
      String corregidaPor,
      Instant corregidaEn) {}
}
