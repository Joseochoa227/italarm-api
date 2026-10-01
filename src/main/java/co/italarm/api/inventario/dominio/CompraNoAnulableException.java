package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La compra no cumple las condiciones para anularse (RF-71, código de RT-05). */
public class CompraNoAnulableException extends NegocioException {

  public static final String CODIGO = "COMPRA_NO_ANULABLE";

  public CompraNoAnulableException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
