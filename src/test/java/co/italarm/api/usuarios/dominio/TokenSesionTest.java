package co.italarm.api.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokenSesionTest {

  private final SecureRandom aleatorio = new SecureRandom();

  @Test
  void generaUnTokenDe256BitsEnBase64Url() {
    TokenSesion token = TokenSesion.generar(aleatorio);

    assertThat(token.valor()).hasSize(43).matches("[A-Za-z0-9_-]+");
  }

  @Test
  void guardaSoloElHashSha256DelToken() {
    TokenSesion token = TokenSesion.generar(aleatorio);

    assertThat(token.hash()).hasSize(64).matches("[0-9a-f]+").isNotEqualTo(token.valor());
    assertThat(TokenSesion.hashDe(token.valor())).isEqualTo(token.hash());
  }

  @Test
  void elHashEsElSha256Estandar() {
    assertThat(TokenSesion.hashDe("abc"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
  }

  @Test
  void cadaTokenEsDistinto() {
    Set<String> tokens = new HashSet<>();
    for (int i = 0; i < 1000; i++) {
      tokens.add(TokenSesion.generar(aleatorio).valor());
    }

    assertThat(tokens).hasSize(1000);
  }

  @Test
  void elTextoDelTokenNoExponeElValor() {
    TokenSesion token = TokenSesion.generar(aleatorio);

    assertThat(token.toString()).doesNotContain(token.valor());
  }
}
