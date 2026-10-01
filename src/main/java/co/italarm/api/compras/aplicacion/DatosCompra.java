package co.italarm.api.compras.aplicacion;

import co.italarm.api.shared.dominio.Moneda;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Compra a registrar (RF-39).
 *
 * @param fecha fecha de la factura; si viene vacía, hoy (P-19)
 */
public record DatosCompra(
    Long proveedorId,
    String numeroFactura,
    LocalDate fecha,
    Moneda moneda,
    List<DatosCompra.Linea> lineas) {

  /** Producto comprado, con su costo unitario en la moneda de la factura. */
  public record Linea(
      Long productoId, BigDecimal cantidad, BigDecimal costoUnitario, List<String> seriales) {}
}
