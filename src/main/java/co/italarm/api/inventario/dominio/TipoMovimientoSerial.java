package co.italarm.api.inventario.dominio;

/** Movimientos del historial de un serial (RF-24). La Fase 4 agrega la instalación. */
public enum TipoMovimientoSerial {
  ENTRADA,
  BAJA,
  ANULACION,
  VENTA,
  ANULACION_VENTA,
  INSTALACION,
  ANULACION_INSTALACION
}
