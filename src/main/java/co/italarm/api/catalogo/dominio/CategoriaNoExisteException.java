package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La categoría indicada no existe. */
public class CategoriaNoExisteException extends NegocioException {

  public static final String CODIGO = "CATEGORIA_NO_EXISTE";

  public CategoriaNoExisteException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
