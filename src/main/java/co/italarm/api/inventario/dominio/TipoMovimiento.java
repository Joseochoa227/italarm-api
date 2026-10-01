package co.italarm.api.inventario.dominio;

/** Tipos de movimiento del kárdex (RF-56). Las fases siguientes agregan ventas e instalaciones. */
public enum TipoMovimiento {
  COMPRA("Compra"),
  ANULACION_COMPRA("Anulación de compra"),
  AJUSTE_ENTRADA("Ajuste"),
  AJUSTE_SALIDA("Ajuste"),
  INVENTARIO_INICIAL("Inventario inicial");

  private final String etiqueta;

  TipoMovimiento(String etiqueta) {
    this.etiqueta = etiqueta;
  }

  public String etiqueta() {
    return etiqueta;
  }
}
