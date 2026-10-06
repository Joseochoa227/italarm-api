package co.italarm.api.inventario.aplicacion;

import co.italarm.api.shared.dominio.DocumentoRef;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Un serial, todo lo que le ha pasado y sus reclamos de garantía (RF-24, RF-125). */
public record HistorialSerialVista(
    SerialVista serial, List<Movimiento> movimientos, List<ReclamosSerial.Reclamo> reclamos) {

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
