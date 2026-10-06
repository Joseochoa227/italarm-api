package co.italarm.api.instalaciones.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La instalación ya estaba anulada. */
public class InstalacionYaAnuladaException extends NegocioException {

  public static final String CODIGO = "INSTALACION_YA_ANULADA";

  public InstalacionYaAnuladaException() {
    super(TipoError.CONFLICTO, CODIGO, "La instalación ya estaba anulada.");
  }
}
