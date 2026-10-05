package co.italarm.api.usuarios.dominio;

import co.italarm.api.shared.dominio.TokenAleatorio;
import java.security.SecureRandom;

/**
 * Token opaco de sesión (decisión P-05). El valor se entrega una sola vez al frontend; en la base
 * de datos solo se guarda su hash SHA-256, para que una copia de la base no permita suplantar a
 * nadie.
 */
public record TokenSesion(String valor, String hash) {

  public static TokenSesion generar(SecureRandom aleatorio) {
    TokenAleatorio token = TokenAleatorio.generar(aleatorio);
    return new TokenSesion(token.valor(), token.hash());
  }

  public static String hashDe(String token) {
    return TokenAleatorio.hashDe(token);
  }

  @Override
  public String toString() {
    return "TokenSesion[hash=" + hash + "]";
  }
}
