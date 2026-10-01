package co.italarm.api.shared.dominio;

/** La cabecera Idempotency-Key está vacía o es demasiado larga. */
public class ClaveIdempotenciaInvalidaException extends NegocioException {

  public ClaveIdempotenciaInvalidaException() {
    super(
        TipoError.VALIDACION,
        CodigoError.VALIDACION,
        "La cabecera Idempotency-Key debe tener entre 1 y 100 caracteres.");
  }
}
