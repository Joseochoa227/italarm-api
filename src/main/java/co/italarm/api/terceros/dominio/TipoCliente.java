package co.italarm.api.terceros.dominio;

/** Tipo de cliente; define el precio que se le aplica (RN-01). */
public enum TipoCliente {
  INSTALADOR(PrecioAplicado.INSTALADOR),
  CLIENTE_FINAL(PrecioAplicado.CLIENTE_FINAL);

  private final PrecioAplicado precio;

  TipoCliente(PrecioAplicado precio) {
    this.precio = precio;
  }

  public PrecioAplicado precio() {
    return precio;
  }
}
