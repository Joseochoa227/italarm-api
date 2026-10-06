package co.italarm.api.cotizaciones.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La acción no se puede hacer en el estado actual de la cotización (sección 3.11). */
public class TransicionNoPermitidaException extends NegocioException {

  public static final String CODIGO = "TRANSICION_NO_PERMITIDA";

  /**
   * @param accion verbo en infinitivo, por ejemplo "aprobar"
   */
  public TransicionNoPermitidaException(String accion, EstadoCotizacion estado) {
    super(
        TipoError.REGLA_NEGOCIO,
        CODIGO,
        "No se puede " + accion + " una cotización " + estado.descripcion() + ".");
  }
}
