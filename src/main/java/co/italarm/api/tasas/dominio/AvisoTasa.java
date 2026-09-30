package co.italarm.api.tasas.dominio;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Aviso cuando la tasa vigente no es la de hoy (RF-07, RF-33). */
public final class AvisoTasa {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  private AvisoTasa() {}

  /**
   * @param ultimaFecha fecha de la última tasa disponible, o null si no hay ninguna
   * @param consultaFallo si la consulta automática de la TRM de hoy falló
   * @return el aviso, o null si la tasa vigente es la de hoy
   */
  public static String para(
      ParMoneda par, LocalDate ultimaFecha, LocalDate hoy, boolean consultaFallo) {
    if (ultimaFecha != null && ultimaFecha.equals(hoy)) {
      return null;
    }
    if (ultimaFecha == null) {
      return par == ParMoneda.USD_VES
          ? "No hay tasa del bolívar registrada. Regístrala para convertir a bolívares."
          : "No hay TRM registrada. Regístrala para convertir a pesos.";
    }
    String fecha = ultimaFecha.format(FECHA);
    if (par == ParMoneda.USD_VES) {
      return "No se ha registrado la tasa del bolívar de hoy. Se está usando la del " + fecha + ".";
    }
    return consultaFallo
        ? "La consulta automática de la TRM falló. Se está usando la del "
            + fecha
            + "; puedes registrarla manualmente."
        : "Aún no se ha obtenido la TRM de hoy. Se está usando la del " + fecha + ".";
  }
}
