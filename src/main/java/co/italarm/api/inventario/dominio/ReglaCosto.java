package co.italarm.api.inventario.dominio;

/** Regla con que cambió el costo de un producto (RF-57, RF-67). */
public enum ReglaCosto {
  /** La compra trajo un costo mayor: todo el stock pasa a ese costo. */
  SUBE,
  /** La compra trajo un costo menor o igual: promedio ponderado. */
  PROMEDIO,
  /** No había unidades en bodega: el costo es el de la factura. */
  SIN_STOCK,
  /** Ajuste de entrada de un producto que nunca tuvo costo (P-25). */
  AJUSTE,
  /** Costo cargado en el inventario inicial (RF-151). */
  INVENTARIO_INICIAL,
  /** Se anuló la compra que lo había cambiado: vuelve al costo anterior (RF-71). */
  ANULACION
}
