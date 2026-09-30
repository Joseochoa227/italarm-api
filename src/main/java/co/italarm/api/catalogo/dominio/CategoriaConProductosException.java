package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Solo se eliminan categorías sin productos (RF-15). */
public class CategoriaConProductosException extends NegocioException {

  public static final String CODIGO = "CATEGORIA_CON_PRODUCTOS";

  public CategoriaConProductosException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
