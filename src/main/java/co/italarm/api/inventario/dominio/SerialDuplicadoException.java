package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** Serial repetido en el documento o ya registrado para el producto (RF-20). */
public class SerialDuplicadoException extends NegocioException {

  public static final String CODIGO = "SERIAL_DUPLICADO";

  public SerialDuplicadoException(String mensaje) {
    super(TipoError.CONFLICTO, CODIGO, mensaje);
  }
}
