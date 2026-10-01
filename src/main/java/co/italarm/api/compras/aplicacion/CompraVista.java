package co.italarm.api.compras.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Detalle de una compra (RF-48).
 *
 * @param estado ACTIVA o ANULADA
 * @param motivoNoAnulable por qué no se puede anular (P-23); vacío si se puede o ya está anulada
 * @param facturaUrl enlace firmado a la factura adjunta, o null
 */
public record CompraVista(
    Long id,
    String consecutivo,
    LocalDate fecha,
    Referencia proveedor,
    String numeroFactura,
    Moneda moneda,
    TasasCompraVista tasas,
    Dinero total,
    Dinero totalUsd,
    String estado,
    List<Linea> lineas,
    String facturaUrl,
    boolean anulable,
    String motivoNoAnulable,
    Anulacion anulacion,
    String registradaPor,
    Instant registradaEn) {

  public record Referencia(Long id, String nombre) {}

  /**
   * Producto de la compra y el cambio de costo que produjo.
   *
   * @param regla SUBE, PROMEDIO o SIN_STOCK
   */
  public record Linea(
      Long productoId,
      String codigo,
      String nombre,
      BigDecimal cantidad,
      String abreviatura,
      Dinero costoUnitario,
      Dinero costoUnitarioUsd,
      Dinero subtotal,
      Dinero costoAnteriorUsd,
      Dinero costoNuevoUsd,
      String regla,
      List<String> seriales) {}

  /** Quién anuló la compra, cuándo y por qué (RF-73). */
  public record Anulacion(String motivo, String usuario, Instant fecha) {}
}
