package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.DocumentoRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Un serial y todo lo que le ha pasado (RF-24). */
public record HistorialSerialVista(SerialVista serial, List<Movimiento> movimientos) {

  /**
   * @param tipo ENTRADA, BAJA o ANULACION (las fases siguientes agregan venta e instalación)
   * @param detalle por ejemplo, "Proveedor: X · Factura Y"
   */
  public record Movimiento(
      String tipo,
      LocalDate fecha,
      DocumentoRef documento,
      String detalle,
      String usuario,
      Instant registradoEn) {}
}
