package co.italarm.api.ventas.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Datos de la venta que no cumplen las reglas (producto repetido, sin productos…). */
public class VentaInvalidaException extends NegocioException {

  public static final String PRODUCTO_REPETIDO = "VENTA_PRODUCTO_REPETIDO";
  public static final String SIN_LINEAS = "VENTA_SIN_LINEAS";

  public VentaInvalidaException(String codigo, String mensaje) {
    super(TipoError.VALIDACION, codigo, mensaje);
  }
}
