package co.italarm.api.tasas.dominio;

import co.italarm.api.shared.dominio.Moneda;

/** Pares de monedas con tasa diaria (sección 3.5). El dólar es la moneda base. */
public enum ParMoneda {
  /** TRM: pesos colombianos por dólar. Se consulta automáticamente (RF-28). */
  USD_COP(Moneda.COP, "TRM"),
  /** Bolívares por dólar. La registra el usuario cada día (RF-29). */
  USD_VES(Moneda.VES, "tasa del bolívar");

  private final Moneda moneda;
  private final String nombre;

  ParMoneda(Moneda moneda, String nombre) {
    this.moneda = moneda;
    this.nombre = nombre;
  }

  public Moneda moneda() {
    return moneda;
  }

  public String nombre() {
    return nombre;
  }
}
