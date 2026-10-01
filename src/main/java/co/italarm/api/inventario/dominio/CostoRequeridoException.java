package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Ajuste de entrada de un producto sin costo: hay que indicar el costo en USD (RF-61). */
public class CostoRequeridoException extends NegocioException {

  public static final String CODIGO = "COSTO_REQUERIDO";

  public CostoRequeridoException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
