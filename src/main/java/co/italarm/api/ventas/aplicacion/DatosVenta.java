package co.italarm.api.ventas.aplicacion;

import co.italarm.api.shared.dominio.Moneda;
import co.italarm.api.shared.dominio.TipoDescuento;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Venta a registrar o a previsualizar (RF-97 a RF-100).
 *
 * @param monedasComprobante otras monedas en que el PDF muestra los totales (P-34)
 */
public record DatosVenta(
    Long clienteId,
    Moneda moneda,
    List<Linea> lineas,
    TipoDescuento descuentoTipo,
    BigDecimal descuentoValor,
    String observaciones,
    Set<Moneda> monedasComprobante) {

  /**
   * Producto vendido.
   *
   * @param cantidad si el producto controla serial y viene vacía, es la cantidad de seriales
   *     (RF-21)
   * @param precioUnitario vacío = el precio sugerido según el tipo de cliente (RN-01)
   */
  public record Linea(
      Long productoId, BigDecimal cantidad, List<String> seriales, BigDecimal precioUnitario) {}
}
