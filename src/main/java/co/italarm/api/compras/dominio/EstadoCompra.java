package co.italarm.api.compras.dominio;

/** Estado de una compra. Una anulada sigue visible, sin sumar en los totales (RF-73). */
public enum EstadoCompra {
  ACTIVA,
  ANULADA
}
