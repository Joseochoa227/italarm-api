package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Ya existe una categoría con ese nombre. */
public class CategoriaDuplicadaException extends NegocioException {

  public static final String CODIGO = "CATEGORIA_DUPLICADA";

  public CategoriaDuplicadaException(String mensaje) {
    super(TipoError.CONFLICTO, CODIGO, mensaje);
  }
}
