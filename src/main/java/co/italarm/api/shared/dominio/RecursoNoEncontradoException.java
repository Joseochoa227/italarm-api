package co.italarm.api.shared.dominio;

/** El recurso pedido no existe. */
public class RecursoNoEncontradoException extends NegocioException {

  public RecursoNoEncontradoException(String mensaje) {
    super(TipoError.NO_ENCONTRADO, CodigoError.RECURSO_NO_ENCONTRADO, mensaje);
  }
}
