package co.italarm.api.inventario.dominio;

/** Motivo de un ajuste de inventario (RF-58). "Otro" exige descripción. */
public enum MotivoAjuste {
  PERDIDA("Pérdida"),
  DANO("Daño"),
  CONTEO_FISICO("Conteo físico"),
  GARANTIA("Garantía"),
  OTRO("Otro");

  private final String etiqueta;

  MotivoAjuste(String etiqueta) {
    this.etiqueta = etiqueta;
  }

  public String etiqueta() {
    return etiqueta;
  }
}
