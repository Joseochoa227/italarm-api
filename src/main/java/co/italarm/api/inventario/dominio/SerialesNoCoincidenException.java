package co.italarm.api.inventario.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La cantidad de seriales no coincide con la cantidad (RF-20). */
public class SerialesNoCoincidenException extends NegocioException {

  public static final String CODIGO = "SERIALES_NO_COINCIDEN";

  public SerialesNoCoincidenException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
