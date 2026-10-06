package co.italarm.api.cotizaciones.dominio;

import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Copia de una versión anterior de la cotización (RF-88, P-46): lo que se le envió al cliente. Se
 * guarda en JSON.
 */
public record ContenidoVersion(
    LocalDate fecha,
    int validezDias,
    LocalDate vence,
    Moneda moneda,
    String descripcion,
    BigDecimal manoDeObra,
    BigDecimal subtotal,
    BigDecimal descuento,
    BigDecimal total,
    String observaciones,
    List<Linea> lineas) {

  public record Linea(
      Long productoId,
      String codigo,
      String descripcion,
      String unidad,
      BigDecimal cantidad,
      BigDecimal precioUnitario,
      BigDecimal subtotal) {}
}
