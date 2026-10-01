package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Una salida supera el stock disponible (RF-65, código de RT-05). */
public class StockInsuficienteException extends NegocioException {

  public static final String CODIGO = "STOCK_INSUFICIENTE";

  public StockInsuficienteException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
