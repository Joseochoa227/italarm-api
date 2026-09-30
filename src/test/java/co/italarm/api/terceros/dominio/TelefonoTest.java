package co.italarm.api.terceros.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** P-10: con indicativo internacional; sin indicativo se asume +57 (Colombia). */
class TelefonoTest {

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource(
      delimiter = '|',
      value = {
        "3001234567          | +573001234567",
        "300 123 4567        | +573001234567",
        "(300) 123-45.67     | +573001234567",
        "+57 300 123 4567    | +573001234567",
        "+58 412 1234567     | +584121234567",
        "0058 412 1234567    | +584121234567",
        "6041234567          | +576041234567"
      })
  void normalizaConIndicativoInternacional(String escrito, String esperado) {
    assertThat(Telefono.normalizar(escrito)).isEqualTo(esperado);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"abc", "12345", "+0573001234567", "300-ABC-4567", "+1234567890123456", " "})
  void rechazaTelefonosInvalidos(String escrito) {
    assertThatThrownBy(() -> Telefono.normalizar(escrito))
        .isInstanceOf(TelefonoInvalidoException.class)
        .hasMessageContaining("indicativo");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "  "})
  void elOpcionalVacioQuedaNulo(String escrito) {
    assertThat(Telefono.normalizarOpcional(escrito)).isNull();
    assertThat(Telefono.normalizarOpcional(null)).isNull();
  }
}
