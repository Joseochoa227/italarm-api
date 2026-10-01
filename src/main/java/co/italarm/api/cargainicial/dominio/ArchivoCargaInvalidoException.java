package co.italarm.api.cargainicial.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** El archivo de la carga inicial no es un Excel (.xlsx) que se pueda leer. */
public class ArchivoCargaInvalidoException extends NegocioException {

  public static final String CODIGO = "ARCHIVO_TIPO_NO_PERMITIDO";

  public ArchivoCargaInvalidoException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
