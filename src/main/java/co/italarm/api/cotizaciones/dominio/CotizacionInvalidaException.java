package co.italarm.api.cotizaciones.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Datos de la cotización que no cumplen las reglas (sin productos, validez inválida…). */
public class CotizacionInvalidaException extends NegocioException {

  public static final String SIN_LINEAS = "COTIZACION_SIN_LINEAS";
  public static final String VACIA = "COTIZACION_VACIA";
  public static final String SIN_DESCRIPCION = "COTIZACION_SIN_DESCRIPCION";
  public static final String MANO_OBRA_EN_VENTA = "COTIZACION_MANO_OBRA_EN_VENTA";
  public static final String VALIDEZ_INVALIDA = "COTIZACION_VALIDEZ_INVALIDA";
  public static final String PRODUCTO_REPETIDO = "COTIZACION_PRODUCTO_REPETIDO";

  public CotizacionInvalidaException(String codigo, String mensaje) {
    super(TipoError.VALIDACION, codigo, mensaje);
  }
}
