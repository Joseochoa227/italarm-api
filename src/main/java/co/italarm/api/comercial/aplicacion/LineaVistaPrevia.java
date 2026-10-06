package co.italarm.api.comercial.aplicacion;

import co.italarm.api.shared.dominio.Dinero;
import co.italarm.api.shared.dominio.MontoEnMonedas;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Una línea de material en la vista previa, con su disponibilidad, precio y costo (RF-64, RF-69,
 * RF-99, RF-108).
 *
 * @param disponible stock actual del producto ("hay 24 und", RF-64)
 * @param avisoStock "Stock insuficiente · quedan N und", o vacío
 * @param avisoPrecio aviso si el precio queda por debajo del costo (P-29), o vacío
 * @param costoUnitarioHoy costo en USD y su equivalente con las tasas de hoy
 * @param costoUnitarioUltimaCompra costo con las tasas de la última compra (P-32), o vacío
 */
public record LineaVistaPrevia(
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
    String avisoPrecio) {

  /** Compra de la que salen las tasas de comparación (RF-69). */
  public record UltimaCompra(
      String consecutivo, LocalDate fecha, BigDecimal trm, BigDecimal tasaVes) {}
}
