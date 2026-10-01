package co.italarm.api.inventario.dominio;

/** Estados de un serial (RF-22). Solo uno En bodega está disponible. */
public enum EstadoSerial {
  EN_BODEGA,
  VENDIDO,
  INSTALADO,
  DADO_DE_BAJA,
  /** Entró con una compra que luego se anuló (P-22); el número se puede volver a registrar. */
  ANULADO
}
