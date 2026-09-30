package co.italarm.api.tasas.aplicacion;

/** La fuente de la TRM no respondió o no tiene la TRM de la fecha pedida. */
public class FuenteTrmNoDisponibleException extends RuntimeException {

  public FuenteTrmNoDisponibleException(String mensaje) {
    super(mensaje);
  }

  public FuenteTrmNoDisponibleException(String mensaje, Throwable causa) {
    super(mensaje, causa);
  }
}
