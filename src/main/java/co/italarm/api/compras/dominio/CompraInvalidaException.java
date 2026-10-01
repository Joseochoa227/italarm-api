package co.italarm.api.compras.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Datos de la compra que no cumplen las reglas (fecha futura, costo 0, producto repetido…). */
public class CompraInvalidaException extends NegocioException {

  public CompraInvalidaException(String codigo, String mensaje) {
    super(TipoError.VALIDACION, codigo, mensaje);
  }
}
