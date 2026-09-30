package co.italarm.api.documentos.dominio;

import java.util.regex.Pattern;

/**
 * Claves de archivo generadas por el sistema, por ejemplo {@code productos/12/foto-3f9a.jpg}. Nunca
 * se usa el nombre que envía el usuario, lo que evita rutas maliciosas.
 */
public final class ClaveArchivo {

  private static final Pattern FORMATO =
      Pattern.compile("[a-z0-9][a-z0-9_.-]*(/[a-z0-9][a-z0-9_.-]*)*");
  private static final int LONGITUD_MAXIMA = 300;

  private ClaveArchivo() {}

  public static String validar(String clave) {
    if (clave == null
        || clave.length() > LONGITUD_MAXIMA
        || clave.contains("..")
        || !FORMATO.matcher(clave).matches()) {
      throw new IllegalArgumentException("Clave de archivo no válida: " + clave);
    }
    return clave;
  }
}
