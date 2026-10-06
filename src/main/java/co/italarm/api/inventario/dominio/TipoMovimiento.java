package co.italarm.api.inventario.dominio;

/** Tipos de movimiento del kárdex (RF-56). */
public enum TipoMovimiento {
  COMPRA("Compra"),
  ANULACION_COMPRA("Anulación de compra"),
  AJUSTE_ENTRADA("Ajuste"),
  AJUSTE_SALIDA("Ajuste"),
  INVENTARIO_INICIAL("Inventario inicial"),
  VENTA("Venta"),
  ANULACION_VENTA("Anulación de venta"),
  INSTALACION("Instalación"),
  ANULACION_INSTALACION("Anulación de instalación");

  private final String etiqueta;

  TipoMovimiento(String etiqueta) {
    this.etiqueta = etiqueta;
  }

  public String etiqueta() {
    return etiqueta;
  }
}
