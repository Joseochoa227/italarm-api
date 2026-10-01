package co.italarm.api.shared.dominio;

/**
 * Cabecera {@code Idempotency-Key} de una creación (RT-07): la misma clave del mismo usuario para
 * la misma operación devuelve el documento ya creado.
 */
public record ClaveIdempotencia(Long usuarioId, String operacion, String valor) {

  public static final int LONGITUD_MAXIMA = 100;
}
