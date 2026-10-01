package co.italarm.api.compras.dominio;

import co.italarm.api.shared.dominio.CantidadInvalidaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Reglas de los datos de una compra antes de tocar el inventario (P-19, P-20, P-21). */
public final class ReglasCompra {

  public static final String FECHA_FUTURA = "COMPRA_FECHA_FUTURA";
  public static final String PRODUCTO_REPETIDO = "COMPRA_PRODUCTO_REPETIDO";
  public static final String COSTO_INVALIDO = "COMPRA_COSTO_INVALIDO";
  public static final String SIN_LINEAS = "COMPRA_SIN_LINEAS";

  private ReglasCompra() {}

  /** Línea a validar: producto, cantidad y costo unitario en la moneda de la factura. */
  public record Linea(Long productoId, BigDecimal cantidad, BigDecimal costoUnitario) {}

  /**
   * Valida la fecha (puede ser anterior a hoy, nunca futura), que haya líneas, que no se repita un
   * producto y que las cantidades y los costos sean mayores que 0. Los decimales de cada cantidad
   * los valida el inventario según la unidad del producto.
   */
  public static void validar(LocalDate fecha, LocalDate hoy, List<Linea> lineas) {
    if (fecha.isAfter(hoy)) {
      throw new CompraInvalidaException(
          FECHA_FUTURA, "La fecha de la compra no puede ser posterior a hoy.");
    }
    if (lineas == null || lineas.isEmpty()) {
      throw new CompraInvalidaException(SIN_LINEAS, "La compra debe tener al menos un producto.");
    }
    Set<Long> vistos = new HashSet<>();
    for (Linea linea : lineas) {
      if (!vistos.add(linea.productoId())) {
        throw new CompraInvalidaException(
            PRODUCTO_REPETIDO,
            "Un producto aparece dos veces en la compra. Si se compró a dos precios, registra dos"
                + " compras.");
      }
      if (linea.cantidad() == null || linea.cantidad().signum() <= 0) {
        throw new CantidadInvalidaException("La cantidad debe ser mayor que 0.");
      }
      if (linea.costoUnitario() == null || linea.costoUnitario().signum() <= 0) {
        throw new CompraInvalidaException(
            COSTO_INVALIDO,
            "El costo unitario debe ser mayor que 0. Las unidades sin costo se ingresan con un"
                + " ajuste de entrada.");
      }
    }
  }
}
