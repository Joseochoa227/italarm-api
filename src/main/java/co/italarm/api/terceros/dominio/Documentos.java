package co.italarm.api.terceros.dominio;

/** Normalización de números de CC y NIT: sin puntos ni espacios (900.123.456-7 → 900123456-7). */
public final class Documentos {

  private Documentos() {}

  public static String normalizar(String numero) {
    if (numero == null) {
      return null;
    }
    String limpio = numero.replaceAll("[\\s.]", "");
    return limpio.isEmpty() ? null : limpio;
  }
}
