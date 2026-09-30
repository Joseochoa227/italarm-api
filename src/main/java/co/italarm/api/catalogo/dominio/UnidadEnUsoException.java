package co.italarm.api.catalogo.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La unidad la usan productos. */
public class UnidadEnUsoException extends NegocioException {

  public static final String CODIGO = "UNIDAD_EN_USO";

  public UnidadEnUsoException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
