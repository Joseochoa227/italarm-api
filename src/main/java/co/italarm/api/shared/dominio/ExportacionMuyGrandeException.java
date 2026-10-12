package co.italarm.api.shared.dominio;

/** La exportación a Excel pasa el máximo de filas por archivo (P-62). */
public class ExportacionMuyGrandeException extends NegocioException {

  public static final String CODIGO = "EXPORTACION_MUY_GRANDE";

  public ExportacionMuyGrandeException(int limite) {
    super(
        TipoError.REGLA_NEGOCIO,
        CODIGO,
        "La exportación pasa de "
            + String.format(java.util.Locale.ROOT, "%,d", limite).replace(',', '.')
            + " filas. Acota las fechas o los filtros.");
  }
}
