package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Ya existe una unidad con ese nombre o abreviatura. */
public class UnidadDuplicadaException extends NegocioException {

  public static final String CODIGO = "UNIDAD_DUPLICADA";

  public UnidadDuplicadaException(String mensaje) {
    super(TipoError.CONFLICTO, CODIGO, mensaje);
  }
}
