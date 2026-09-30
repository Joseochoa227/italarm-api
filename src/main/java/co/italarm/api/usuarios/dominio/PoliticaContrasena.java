package co.italarm.api.usuarios.dominio;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Requisitos mínimos de una contraseña (P-07, P-08): al menos 8 caracteres, una mayúscula, una
 * minúscula, un número y un signo. El máximo de 64 caracteres (y 72 bytes) lo impone BCrypt.
 */
public final class PoliticaContrasena {

  public static final int LONGITUD_MINIMA = 8;
  public static final int LONGITUD_MAXIMA = 64;
  private static final int BYTES_MAXIMOS_BCRYPT = 72;

  public static final String DESCRIPCION =
      "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y"
          + " un signo.";

  private PoliticaContrasena() {}

  public static boolean cumple(String contrasena) {
    return incumplimientos(contrasena).isEmpty();
  }

  /** Requisitos que la contraseña no cumple, en el orden en que se le explican al usuario. */
  public static List<String> incumplimientos(String contrasena) {
    String valor = contrasena == null ? "" : contrasena;
    List<String> faltantes = new ArrayList<>();
    if (valor.length() < LONGITUD_MINIMA) {
      faltantes.add("al menos 8 caracteres");
    }
    if (valor.length() > LONGITUD_MAXIMA
        || valor.getBytes(StandardCharsets.UTF_8).length > BYTES_MAXIMOS_BCRYPT) {
      faltantes.add("máximo 64 caracteres");
    }
    if (valor.chars().noneMatch(Character::isUpperCase)) {
      faltantes.add("una mayúscula");
    }
    if (valor.chars().noneMatch(Character::isLowerCase)) {
      faltantes.add("una minúscula");
    }
    if (valor.chars().noneMatch(Character::isDigit)) {
      faltantes.add("un número");
    }
    if (valor.chars().noneMatch(PoliticaContrasena::esSigno)) {
      faltantes.add("un signo");
    }
    return faltantes;
  }

  private static boolean esSigno(int caracter) {
    return !Character.isLetterOrDigit(caracter) && !Character.isWhitespace(caracter);
  }
}
