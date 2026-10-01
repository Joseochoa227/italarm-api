package co.italarm.api.shared.dominio;

/** La cantidad no respeta la unidad de medida (P-09). */
public class CantidadInvalidaException extends NegocioException {

  public static final String CODIGO = "CANTIDAD_INVALIDA";

  public CantidadInvalidaException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
