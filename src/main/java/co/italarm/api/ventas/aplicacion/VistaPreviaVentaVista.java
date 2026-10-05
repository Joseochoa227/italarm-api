package co.italarm.api.ventas.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que pasaría al guardar la venta, sin guardar nada (RF-98 a RF-101). Es el valor oficial que
 * muestra el frontend (BF-06).
 *
 * @param puedeGuardar false si alguna línea no tiene stock suficiente (RF-101)
 * @param avisos por ejemplo, que la tasa del bolívar no es la de hoy (RF-33)
 */
public record VistaPreviaVentaVista(
    LocalDate fecha,
    ClienteVentaVista cliente,
    Moneda moneda,
    TasasVentaVista tasas,
    List<String> avisos,
    List<Linea> lineas,
    ResumenVentaVista resumen,
    boolean puedeGuardar) {

  /**
   * Una línea con su disponibilidad, precio y costo (RF-99, RF-69).
   *
   * @param disponible stock actual del producto ("hay 24 und", RF-64)
   * @param avisoStock "Stock insuficiente · quedan N und", o vacío
   * @param avisoPrecio aviso si el precio queda por debajo del costo (P-29), o vacío
   * @param costoUnitarioHoy costo en USD y su equivalente con las tasas de hoy
   * @param costoUnitarioUltimaCompra costo con las tasas de la última compra (P-32), o vacío
   */
  public record Linea(
      Long productoId,
      String codigo,
      String nombre,
      String abreviatura,
      boolean controlaSerial,
      BigDecimal cantidad,
      BigDecimal disponible,
      String avisoStock,
      Dinero precioSugerido,
      Dinero precioUnitario,
      MontoEnMonedas subtotal,
      MontoEnMonedas costoUnitarioHoy,
      MontoEnMonedas costoUnitarioUltimaCompra,
      UltimaCompra ultimaCompra,
      String avisoPrecio) {}

  /** Compra de la que salen las tasas de comparación (RF-69). */
  public record UltimaCompra(
      String consecutivo, LocalDate fecha, BigDecimal trm, BigDecimal tasaVes) {}
}
