package co.italarm.api.documentos.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

public class ArchivoTipoNoPermitidoException extends NegocioException {

  public static final String CODIGO = "ARCHIVO_TIPO_NO_PERMITIDO";

  public ArchivoTipoNoPermitidoException() {
    super(TipoError.VALIDACION, CODIGO, "El archivo debe ser una imagen JPEG, PNG o WebP.");
  }
}
