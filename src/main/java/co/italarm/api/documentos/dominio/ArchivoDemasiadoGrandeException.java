package co.italarm.api.documentos.dominio;

import co.italarm.api.shared.dominio.CodigoError;
import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

public class ArchivoDemasiadoGrandeException extends NegocioException {

  public ArchivoDemasiadoGrandeException(String mensaje) {
    super(TipoError.VALIDACION, CodigoError.ARCHIVO_DEMASIADO_GRANDE, mensaje);
  }
}
