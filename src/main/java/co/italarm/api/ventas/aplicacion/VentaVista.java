package co.italarm.api.ventas.aplicacion;

import co.italarm.api.comercial.aplicacion.ClienteDocumentoVista;
import co.italarm.api.comercial.aplicacion.CotizacionOrigenVista;
import co.italarm.api.comercial.aplicacion.ResumenCobroVista;
import co.italarm.api.comercial.aplicacion.TasasDocumentoVista;
import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Detalle de una venta (RF-105).
 *
 * @param descuentoTipo PORCENTAJE o VALOR
 * @param descuentoValor lo que se escribió (porcentaje o valor)
 * @param resumen totales en las tres monedas con las tasas guardadas en la venta
 * @param cotizacion cotización de la que salió la venta (RF-95), o vacío
 * @param estado ACTIVA o ANULADA
 */
public record VentaVista(
    Long id,
    String consecutivo,
    LocalDate fecha,
    ClienteDocumentoVista cliente,
    Moneda moneda,
    TasasDocumentoVista tasas,
    List<Linea> lineas,
    String descuentoTipo,
    BigDecimal descuentoValor,
    ResumenCobroVista resumen,
    Dinero total,
    Dinero utilidad,
    BigDecimal porcentajeUtilidad,
    String observaciones,
    Set<Moneda> monedasComprobante,
    CotizacionOrigenVista cotizacion,
    String estado,
    Anulacion anulacion,
    String registradaPor,
    Instant registradaEn,
    long version) {

  /**
   * Producto vendido, con el costo en USD al momento de la salida (RF-68) y sus seriales.
   *
   * @param seriales con el fin de su garantía (RF-23)
   */
  public record Linea(
      Long productoId,
      String codigo,
      String descripcion,
      String unidad,
      BigDecimal cantidad,
      Dinero precioUnitario,
      Dinero precioSugerido,
      Dinero subtotal,
      Dinero costoUnitarioUsd,
      List<SerialVendido> seriales) {}

  public record SerialVendido(Long id, String numero, LocalDate vencimientoGarantia) {}

  /** Quién anuló la venta, cuándo y por qué (RF-73). */
  public record Anulacion(String motivo, String usuario, Instant fecha) {}
}
