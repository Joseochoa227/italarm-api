package co.italarm.api.documentos.aplicacion;

import java.time.LocalDate;
import java.util.List;

/**
 * Contenido de un comprobante o cotización en PDF (RF-126 a RF-132), con los textos ya formateados.
 * El mismo formato sirve para ventas, instalaciones y cotizaciones.
 *
 * @param titulo por ejemplo "Comprobante de venta"
 * @param validoHasta solo en cotizaciones
 * @param totales en la moneda del documento; el último es el total
 * @param equivalentes totales en otras monedas con la tasa y su fecha (RF-130)
 * @param pie condiciones y leyendas (RF-131)
 * @param marca texto grande cruzado sobre la página, por ejemplo "ANULADA", o vacío
 */
public record DocumentoPdf(
    Empresa empresa,
    String titulo,
    String consecutivo,
    LocalDate fecha,
    LocalDate validoHasta,
    Cliente cliente,
    List<Item> items,
    List<Total> totales,
    List<String> equivalentes,
    String observaciones,
    List<String> pie,
    String marca) {

  /**
   * @param logo imagen del logo (JPEG, PNG o WebP), o vacío
   */
  public record Empresa(
      String nombre,
      String lema,
      String nit,
      String ciudad,
      String telefono,
      String correo,
      byte[] logo) {}

  public record Cliente(String nombre, String documento, String direccion, String telefono) {}

  /**
   * Una fila de la tabla de ítems (RF-129).
   *
   * @param cantidad con su unidad, por ejemplo "12,5 m"
   * @param detalles líneas pequeñas debajo de la descripción (seriales y garantía)
   */
  public record Item(
      String descripcion,
      String cantidad,
      String valorUnitario,
      String total,
      List<String> detalles) {}

  public record Total(String etiqueta, String valor) {}
}
