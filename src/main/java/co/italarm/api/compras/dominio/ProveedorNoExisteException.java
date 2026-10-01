package co.italarm.api.compras.dominio;

import co.italarm.api.shared.dominio.NegocioException;
import co.italarm.api.shared.dominio.TipoError;

/** La compra indica un proveedor que no existe. */
public class ProveedorNoExisteException extends NegocioException {

  public static final String CODIGO = "PROVEEDOR_NO_EXISTE";

  public ProveedorNoExisteException() {
    super(TipoError.VALIDACION, CODIGO, "El proveedor indicado no existe.");
  }
}
