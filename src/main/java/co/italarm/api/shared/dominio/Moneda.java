package co.italarm.api.shared.dominio;

/** Monedas que maneja el negocio. USD es la moneda principal (sección 3.5). */
public enum Moneda {
  USD("US$", 2),
  COP("$", 0),
  VES("Bs", 2);

  private final String simbolo;
  private final int decimalesVisibles;

  Moneda(String simbolo, int decimalesVisibles) {
    this.simbolo = simbolo;
    this.decimalesVisibles = decimalesVisibles;
  }

  public String simbolo() {
    return simbolo;
  }

  /** Decimales con que se muestra la moneda (RNF-04). */
  public int decimalesVisibles() {
    return decimalesVisibles;
  }
}
