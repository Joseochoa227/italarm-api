package co.italarm.api.documentos.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ClaveArchivoTest {

  @ParameterizedTest
  @ValueSource(strings = {"productos/1/foto-ab12.jpg", "configuracion/logo-9f.png", "a.webp"})
  void aceptaClavesGeneradasPorElSistema(String clave) {
    assertThat(ClaveArchivo.validar(clave)).isEqualTo(clave);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "../etc/passwd",
        "productos/../../x",
        "/absoluta.jpg",
        "con espacio.jpg",
        "",
        "A.JPG"
      })
  void rechazaClavesPeligrosasOInvalidas(String clave) {
    assertThatThrownBy(() -> ClaveArchivo.validar(clave))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
