package co.italarm.api.cotizaciones.dominio;

/** Ciclo de vida de una cotización (sección 3.11). */
public enum EstadoCotizacion {
  BORRADOR("en borrador"),
  EN_EVALUACION("en evaluación"),
  APROBADA("aprobada"),
  CONVERTIDA("convertida"),
  RECHAZADA("rechazada"),
  VENCIDA("vencida");

  private final String descripcion;

  EstadoCotizacion(String descripcion) {
    this.descripcion = descripcion;
  }

  /** "en evaluación", para los mensajes. */
  public String descripcion() {
    return descripcion;
  }
}
