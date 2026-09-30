package co.italarm.api.shared.dominio;

/** Normalización de textos que escribe el usuario. */
public final class Textos {

  private Textos() {}

  /** Quita espacios a los lados y repetidos; un texto vacío queda como {@code null}. */
  public static String limpiar(String texto) {
    if (texto == null) {
      return null;
    }
    String limpio = texto.trim().replaceAll("\\s+", " ");
    return limpio.isEmpty() ? null : limpio;
  }
}
