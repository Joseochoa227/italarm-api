package co.italarm.api.inventario.dominio;

/**
 * Movimientos del historial de un serial (RF-24). Las fases siguientes agregan venta e instalación.
 */
public enum TipoMovimientoSerial {
  ENTRADA,
  BAJA,
  ANULACION
}
