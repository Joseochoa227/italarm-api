package co.italarm.api.terceros.dominio;

/** Precio de lista que se le aplica a un cliente según su tipo (RF-75, RF-78). */
public enum PrecioAplicado {
  INSTALADOR("Se le aplicará el precio instalador"),
  CLIENTE_FINAL("Se le aplicará el precio cliente final");

  private final String descripcion;

  PrecioAplicado(String descripcion) {
    this.descripcion = descripcion;
  }

  public String descripcion() {
    return descripcion;
  }
}
