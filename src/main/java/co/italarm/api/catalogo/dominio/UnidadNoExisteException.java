package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La unidad de medida indicada no existe. */
public class UnidadNoExisteException extends NegocioException {

  public static final String CODIGO = "UNIDAD_NO_EXISTE";

  public UnidadNoExisteException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
