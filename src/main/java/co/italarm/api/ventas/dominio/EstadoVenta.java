package co.italarm.api.ventas.dominio;

/** Una venta guardada no se edita ni se borra: solo se anula (RF-106). */
public enum EstadoVenta {
  ACTIVA,
  ANULADA
}
