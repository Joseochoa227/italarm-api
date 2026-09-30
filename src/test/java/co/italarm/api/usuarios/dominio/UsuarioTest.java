package co.italarm.api.usuarios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UsuarioTest {

  @Test
  void normalizaElCorreoAMinusculasSinEspacios() {
    assertThat(Usuario.normalizarCorreo("  JoseOchoa227@Gmail.COM "))
        .isEqualTo("joseochoa227@gmail.com");
  }
}
