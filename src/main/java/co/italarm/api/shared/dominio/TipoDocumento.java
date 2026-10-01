package co.italarm.api.shared.dominio;

/**
 * Documentos con consecutivo automático (RN-09). El número sale de una secuencia de PostgreSQL por
 * tipo y el formato se aplica al mostrar (BP-11).
 */
public enum TipoDocumento {
  COMPRA("C-", 4),
  AJUSTE("AJ-", 3),
  INVENTARIO_INICIAL("II-", 3),
  VENTA("V-", 4),
  INSTALACION("I-", 4),
  COTIZACION("COT-", 4);

  private final String prefijo;
  private final int digitos;

  TipoDocumento(String prefijo, int digitos) {
    this.prefijo = prefijo;
    this.digitos = digitos;
  }

  public String consecutivo(long numero) {
    return prefijo + String.format("%0" + digitos + "d", numero);
  }
}
