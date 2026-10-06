package co.italarm.api.cotizaciones.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/**
 * La cotización no se puede convertir en el documento pedido: no está aprobada, ya fue convertida
 * (RN-14), o el documento es de otro tipo o de otro cliente (P-54).
 */
public class CotizacionNoConvertibleException extends NegocioException {

  public static final String CODIGO = "COTIZACION_NO_CONVERTIBLE";

  public CotizacionNoConvertibleException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
