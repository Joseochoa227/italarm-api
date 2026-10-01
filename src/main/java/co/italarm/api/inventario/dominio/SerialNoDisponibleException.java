package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** El serial no está en bodega (RF-22, código de RT-05). */
public class SerialNoDisponibleException extends NegocioException {

  public static final String CODIGO = "SERIAL_NO_DISPONIBLE";

  public SerialNoDisponibleException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
