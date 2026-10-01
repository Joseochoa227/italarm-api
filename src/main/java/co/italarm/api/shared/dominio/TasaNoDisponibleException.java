package co.italarm.api.shared.dominio;

/** Falta la tasa necesaria para convertir entre monedas. */
public class TasaNoDisponibleException extends NegocioException {

  public static final String CODIGO = "TASA_NO_DISPONIBLE";

  public TasaNoDisponibleException(String mensaje) {
    super(TipoError.REGLA_NEGOCIO, CODIGO, mensaje);
  }
}
