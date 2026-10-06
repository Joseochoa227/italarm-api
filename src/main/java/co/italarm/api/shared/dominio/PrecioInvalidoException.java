package co.italarm.api.shared.dominio;

/** Un precio o la mano de obra quedaron negativos (P-29). */
public class PrecioInvalidoException extends NegocioException {

  public static final String CODIGO = "PRECIO_INVALIDO";

  public PrecioInvalidoException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
