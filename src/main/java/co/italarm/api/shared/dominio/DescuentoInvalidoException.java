package co.italarm.api.shared.dominio;

/** Descuento negativo, de más del 100 % o mayor que el subtotal (P-30). */
public class DescuentoInvalidoException extends NegocioException {

  public static final String CODIGO = "DESCUENTO_INVALIDO";

  public DescuentoInvalidoException(String mensaje) {
    super(TipoError.VALIDACION, CODIGO, mensaje);
  }
}
