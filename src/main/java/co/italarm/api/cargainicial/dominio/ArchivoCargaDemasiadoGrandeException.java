package co.italarm.api.cargainicial.dominio;

import co.italarm.api.shared.dominio.CodigoError;
import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** El archivo de la carga inicial supera los 5 MB. */
public class ArchivoCargaDemasiadoGrandeException extends NegocioException {

  public ArchivoCargaDemasiadoGrandeException() {
    super(
        TipoError.VALIDACION,
        CodigoError.ARCHIVO_DEMASIADO_GRANDE,
        "El archivo supera los 5 MB permitidos.");
  }
}
