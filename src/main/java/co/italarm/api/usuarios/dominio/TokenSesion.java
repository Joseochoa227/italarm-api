package co.italarm.api.usuarios.dominio;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Token opaco de sesión (decisión P-05). El valor se entrega una sola vez al frontend; en la base
 * de datos solo se guarda su hash SHA-256, para que una copia de la base no permita suplantar a
 * nadie.
 */
public record TokenSesion(String valor, String hash) {

  private static final int BYTES_ALEATORIOS = 32;

  public static TokenSesion generar(SecureRandom aleatorio) {
    byte[] bytes = new byte[BYTES_ALEATORIOS];
    aleatorio.nextBytes(bytes);
    String valor = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    return new TokenSesion(valor, hashDe(valor));
  }

  public static String hashDe(String token) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(sha256.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("La JVM no soporta SHA-256", e);
    }
  }

  @Override
  public String toString() {
    return "TokenSesion[hash=" + hash + "]";
  }
}
